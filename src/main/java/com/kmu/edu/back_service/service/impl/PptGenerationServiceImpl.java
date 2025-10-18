package com.kmu.edu.back_service.service.impl;

import com.kmu.edu.back_service.dto.request.PptGenerationRequest;
import com.kmu.edu.back_service.dto.response.PptGenerationResponse;
import com.kmu.edu.back_service.dto.response.PptTemplateResponse;
import com.kmu.edu.back_service.entity.PptGeneration;
import com.kmu.edu.back_service.entity.PptTemplate;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.constant.Constants;
import com.kmu.edu.back_service.mapper.PptGenerationMapper;
import com.kmu.edu.back_service.mapper.PptTemplateMapper;
import com.kmu.edu.back_service.service.MinioStorageService;
import com.kmu.edu.back_service.service.PptGenerationService;
import com.kmu.edu.back_service.service.SiliconAIService;
import com.kmu.edu.back_service.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.TextParagraph;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PPT生成服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PptGenerationServiceImpl implements PptGenerationService {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_\\-]+)\\s*\\}\\}");
    
    private final PptGenerationMapper pptGenerationMapper;
    private final PptTemplateMapper pptTemplateMapper;
    private final SiliconAIService siliconAIService;
    private final MinioStorageService minioStorageService;
    private final com.kmu.edu.back_service.service.storage.MinioStorageService storageMinioService;
    private final SysUserService userService;
    
    @Override
    public PptGenerationResponse generatePpt(Long userId, PptGenerationRequest request) {
        log.info("开始生成PPT，用户ID: {}, 标题: {}", userId, request.getTitle());
        // 配额校验（按服务类型PPT）
        userService.checkDailyLimit(userId, Constants.ServiceType.PPT);

        // 扣减 tokens（PPT 每次 20）
        if (!userService.useTokens(userId, Constants.TokenCost.PPT)) {
            throw new BusinessException("Token不足，无法生成PPT");
        }

        try {
            // 创建生成记录
            PptGeneration pptGeneration = new PptGeneration();
            pptGeneration.setUserId(userId);
            pptGeneration.setTitle(request.getTitle());
            pptGeneration.setPrompt(request.getPrompt());
            pptGeneration.setTemplateId(request.getTemplateId());
            pptGeneration.setSlideCount(request.getSlideCount());
            pptGeneration.setStatus(0); // 生成中
            pptGeneration.setCreateTime(LocalDateTime.now());

            pptGenerationMapper.insert(pptGeneration);

            // 异步生成PPT
            generatePptAsync(pptGeneration);

            return convertToResponse(pptGeneration);
        } catch (Exception e) {
            // 失败返还 tokens
            try { userService.addTokens(userId, Constants.TokenCost.PPT); } catch (Exception ignore) {}
            throw e;
        }
    }
    
    @Async
    public void generatePptAsync(PptGeneration pptGeneration) {
        try {
            log.info("异步生成PPT开始，记录ID: {}", pptGeneration.getId());
            
            long startTime = System.currentTimeMillis();

            // 先解析模板，供 AI 参考
            String templateUrl = null;
            byte[] templateBytes = null;
            TemplateInfo templateInfo = null;
            if (pptGeneration.getTemplateId() != null) {
                try {
                    String tid = pptGeneration.getTemplateId();
                    Long idLong = null;
                    try { idLong = Long.valueOf(tid); } catch (Exception ignore) {}
                    if (idLong != null) {
                        PptTemplate tpl = pptTemplateMapper.selectById(idLong);
                        if (tpl != null && tpl.getTemplateUrl() != null && !tpl.getTemplateUrl().isBlank()) {
                            templateUrl = tpl.getTemplateUrl();
                        }
                    } else if (tid.startsWith("http://") || tid.startsWith("https://")) {
                        templateUrl = tid;
                    }
                    if (templateUrl != null && !templateUrl.isBlank()) {
                        String downloadUrl = templateUrl;
                        try { downloadUrl = storageMinioService.generatePresignedGetUrlForPublicUrl(templateUrl, 300); } catch (Throwable ignore) {}
                        templateBytes = cn.hutool.http.HttpUtil.downloadBytes(downloadUrl);
                        templateInfo = inspectTemplate(templateBytes);
                    }
                } catch (Exception e) {
                    log.warn("模板解析失败，将使用空白主题: {}", e.getMessage());
                }
            }

            // 构建包含模板信息的 AI 提示词
            String enhancedPrompt = buildPptPrompt(pptGeneration, templateInfo);
            
            // 调用AI服务生成PPT内容结构（期望 JSON 计划）
            String pptContent = siliconAIService.chatCompletion(enhancedPrompt, null);
            // 基于 AI 文本 + 模板字节 生成 PPTX 文件与首张缩略图
            GeneratedPptFiles files = buildPptxAndThumb(pptGeneration, pptContent, templateBytes, templateInfo);
            // 上传到 MinIO（稳定可访问的 URL）
            String objBase = "ppt/" + pptGeneration.getUserId() + "/" + pptGeneration.getId();
            String pptUrl = minioStorageService.upload(objBase + ".pptx", new ByteArrayInputStream(files.pptxBytes()), files.pptxBytes().length, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            String thumbUrl = null;
            if (files.thumbBytes() != null) {
                thumbUrl = minioStorageService.upload(objBase + "_thumb.png", new ByteArrayInputStream(files.thumbBytes()), files.thumbBytes().length, "image/png");
            }
            
            long endTime = System.currentTimeMillis();
            int generationTime = (int) ((endTime - startTime) / 1000);
            
            // 更新生成状态
            pptGeneration.setStatus(1); // 成功
            pptGeneration.setGenerationTime(generationTime);
            pptGeneration.setPptUrl(pptUrl);
            pptGeneration.setPdfUrl(null); // 暂不生成PDF
            pptGeneration.setThumbnailUrl(thumbUrl);
            
            pptGenerationMapper.updateById(pptGeneration);
            
            log.info("PPT生成完成，记录ID: {}, 耗时: {}秒", pptGeneration.getId(), generationTime);
            
        } catch (Exception e) {
            log.error("PPT生成失败，记录ID: {}", pptGeneration.getId(), e);
            
            // 更新失败状态
            pptGeneration.setStatus(2); // 失败
            pptGeneration.setErrorMessage(e.getMessage());
            pptGenerationMapper.updateById(pptGeneration);
        }
    }
    
    private String buildPptPrompt(PptGeneration pptGeneration, TemplateInfo templateInfo) {
        if (templateInfo != null && templateInfo.hasPlaceholders()) {
            return buildPlaceholderPrompt(pptGeneration, templateInfo);
        }
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请生成一个关于'").append(pptGeneration.getTitle()).append("'的PPT大纲。");
        promptBuilder.append("\n\n用户要求：").append(pptGeneration.getPrompt());
        
        if (pptGeneration.getSlideCount() != null) {
            promptBuilder.append("\n\n期望幻灯片数量：").append(pptGeneration.getSlideCount()).append("页");
        }
        if (templateInfo != null) {
            promptBuilder.append("\n\n模板信息：");
            promptBuilder.append("\n- 页面尺寸：").append(templateInfo.pageWidth).append("x").append(templateInfo.pageHeight);
            promptBuilder.append("\n- 可用布局：");
            for (LayoutInfo li : templateInfo.layouts) {
                promptBuilder.append("\n  • ").append(li.typeName != null ? li.typeName : li.name)
                        .append("（占位符：").append(String.join(",", li.placeholders)).append(")");
            }
            if (templateInfo.samples != null && !templateInfo.samples.isEmpty()) {
                promptBuilder.append("\n- 示例内容结构：");
                int idx = 1;
                for (SlideSample sample : templateInfo.samples) {
                    promptBuilder.append("\n  幻灯片").append(idx++).append("（布局：");
                    if (sample.layoutName != null && !sample.layoutName.isBlank()) {
                        promptBuilder.append(sample.layoutName);
                    } else if (sample.typeName != null) {
                        promptBuilder.append(sample.typeName);
                    } else {
                        promptBuilder.append("未知");
                    }
                    promptBuilder.append("）");
                    if (sample.texts != null) {
                        for (SampleText text : sample.texts) {
                            promptBuilder.append("\n    • ");
                            if (text.placeholderType != null) {
                                promptBuilder.append(text.placeholderType).append("：");
                            }
                            String cleaned = text.text != null ? text.text.replaceAll("\s+", " ") : "";
                            if (cleaned.length() > 80) {
                                cleaned = cleaned.substring(0, 80) + "...";
                            }
                            promptBuilder.append(cleaned.isEmpty() ? "(空)" : cleaned);
                        }
                    }
                }
                promptBuilder.append("\n请参考模板的文本层级和数量，输出与之对应的内容。");
            }
        }
        // 要求严格 JSON 计划输出
        promptBuilder.append("\n\n请根据上述需求和模板布局，输出严格的 JSON（不要附加解释），例如：\n");
        promptBuilder.append("{\n  \"slides\": [\n    { \"layout\": \"TITLE\", \"title\": \"封面标题\", \"subtitle\": \"可选\" },\n    { \"layout\": \"TITLE_AND_CONTENT\", \"title\": \"目录\", \"bullets\": [\"要点1\", \"要点2\"] }\n  ]\n}\n");
        promptBuilder.append("其中 layout 尽量从模板中存在的布局取值（如 TITLE、TITLE_ONLY、TITLE_AND_CONTENT、SECTION_HEADER 等）。封面页使用 TITLE，内容页使用 TITLE_AND_CONTENT。每页要点 3-6 条。");
        
        return promptBuilder.toString();
    }

    private String buildPlaceholderPrompt(PptGeneration pptGeneration, TemplateInfo templateInfo) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一名专业的中文PPT撰稿助手。请根据主题‘")
                .append(pptGeneration.getTitle()).append("’和以下需求生成文字内容。");
        if (pptGeneration.getPrompt() != null && !pptGeneration.getPrompt().isBlank()) {
            promptBuilder.append("\n\n补充要求：").append(pptGeneration.getPrompt());
        }
        promptBuilder.append("\n\n该模板通过占位符指定可替换位置，请为每个占位符提供合适的文本。");
        promptBuilder.append("\n占位符列表（按页顺序）：");
        if (templateInfo.placeholders != null) {
            for (PlaceholderDescriptor descriptor : templateInfo.placeholders) {
                promptBuilder.append("\n- 第").append(descriptor.slideIndex + 1).append("页：{{")
                        .append(descriptor.name).append("}}");
                if (descriptor.list) {
                    promptBuilder.append("（请输出数组，包含3-6条要点）");
                } else {
                    promptBuilder.append("（输出一段简洁文本，50字以内）");
                }
                if (descriptor.sampleText != null && !descriptor.sampleText.isBlank()) {
                    promptBuilder.append("，原模板示例：").append(descriptor.sampleText);
                }
            }
        }
        promptBuilder.append("\n\n请输出严格的 JSON，对象的键必须为占位符名称。例如：");
        promptBuilder.append("\n{");
        promptBuilder.append("\n  \"cover_title\": \"故宫概览\",");
        promptBuilder.append("\n  \"cover_bullets\": [\"背景介绍\", \"建筑结构\"]");
        promptBuilder.append("\n}");
        promptBuilder.append("\n注意：");
        promptBuilder.append("\n1. JSON 外不要附加任何解释或 Markdown。");
        promptBuilder.append("\n2. 仅使用给定占位符的键，多余键会被忽略。");
        promptBuilder.append("\n3. 对于数组占位符，给出 3-6 条要点，单条不超过 30 字。");
        promptBuilder.append("\n4. 文本占位符保持专业、语法正确，不要包含换行符。");
        return promptBuilder.toString();
    }
    
    private GeneratedPptFiles buildPptxAndThumb(PptGeneration pg, String aiContent, byte[] templateBytes, TemplateInfo templateInfo) throws Exception {
        String title = pg.getTitle() != null ? pg.getTitle().trim() : "AI PPT";
        String content = aiContent != null ? aiContent : "";

        boolean placeholderMode = templateInfo != null && templateInfo.hasPlaceholders();
        Map<String, PlaceholderValue> placeholderValues = Collections.emptyMap();
        if (placeholderMode) {
            placeholderValues = parsePlaceholderValues(content, templateInfo);
            if (placeholderValues.isEmpty()) {
                log.warn("占位符模式解析失败，回退到布局模式");
                placeholderMode = false;
            }
        }

        XMLSlideShow ppt;
        XMLSlideShow templateShow = null;
        if (templateBytes != null) {
            ppt = new XMLSlideShow(new ByteArrayInputStream(templateBytes));
            if (!placeholderMode) {
                templateShow = new XMLSlideShow(new ByteArrayInputStream(templateBytes));
                for (int i = ppt.getSlides().size() - 1; i >= 0; i--) { ppt.removeSlide(i); }
            }
        } else {
            ppt = new XMLSlideShow();
            ppt.setPageSize(new Dimension(1280, 720));
        }

        if (placeholderMode) {
            applyPlaceholderValues(ppt, placeholderValues, templateInfo);
        } else {
            PptPlan plan = parsePlan(content);
            if (plan == null || plan.slides == null || plan.slides.isEmpty()) {
                plan = buildFallbackPlan(pg, content);
            }

            List<XSLFSlide> templateSlides = templateShow != null ? templateShow.getSlides() : Collections.emptyList();
            int totalSlides = plan.slides.size();
            for (int idx = 0; idx < totalSlides; idx++) {
                SlidePlan sp = plan.slides.get(idx);
                boolean isCover = idx == 0;
                XSLFSlide slide;
                if (!templateSlides.isEmpty()) {
                    XSLFSlide templateSlide = templateSlides.get(Math.min(idx, templateSlides.size() - 1));
                    slide = cloneTemplateSlide(ppt, templateSlide);
                } else {
                    String layoutKey = sp.layout != null ? sp.layout.toUpperCase() : (isCover ? "TITLE" : "TITLE_AND_CONTENT");
                    XSLFSlideLayout layout = findLayoutAcrossMasters(ppt, new String[]{layoutKey, isCover ? "TITLE" : "TITLE_AND_CONTENT", "TITLE_ONLY"});
                    slide = (layout != null) ? ppt.createSlide(layout) : ppt.createSlide();
                }
                applySlideContent(slide, sp, isCover, title);
            }
        }

        // 输出 PPTX bytes
        ByteArrayOutputStream pptOut = new ByteArrayOutputStream();
        ppt.write(pptOut);
    try { if (templateShow != null) templateShow.close(); } catch (Throwable ignore) {}
    ppt.close();
        byte[] pptBytes = pptOut.toByteArray();

        // 生成首张缩略图（渲染主题）
        byte[] thumbBytes = null;
        try {
            XMLSlideShow renderShow = new XMLSlideShow(new ByteArrayInputStream(pptBytes));
            Dimension dim = renderShow.getPageSize();
            if (dim == null) dim = new Dimension(1280, 720);
            BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(Color.WHITE);
            g.fillRect(0, 0, dim.width, dim.height);
            if (!renderShow.getSlides().isEmpty()) {
                renderShow.getSlides().get(0).draw(g);
            }
            g.dispose();
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            ImageIO.write(img, "png", pngOut);
            thumbBytes = pngOut.toByteArray();
            renderShow.close();
        } catch (Throwable t) {
            log.warn("PPT缩略图生成失败（忽略）: {}", t.getMessage());
        }

        return new GeneratedPptFiles(pptBytes, thumbBytes);
    }

    private XSLFSlideLayout findLayout(XSLFSlideMaster master, String[] preferredTypes) {
        if (master == null) return null;
    XSLFSlideLayout[] layouts = master.getSlideLayouts();
    if (layouts == null || layouts.length == 0) return null;
        for (String want : preferredTypes) {
            for (XSLFSlideLayout l : layouts) {
                try {
                    String type = String.valueOf(l.getType()); // 避免直接依赖 SlideLayout 枚举
                    if (want.equalsIgnoreCase(type)) return l;
                } catch (Throwable ignore) {}
                try {
                    String name = l.getName();
                    if (name != null && name.toUpperCase().contains(want)) return l;
                } catch (Throwable ignore) {}
            }
        }
        return layouts[0];
    }

    private XSLFSlideLayout findLayoutAcrossMasters(XMLSlideShow show, String[] preferredTypes) {
        if (show == null) return null;
        List<XSLFSlideMaster> masters = show.getSlideMasters();
        if (masters == null || masters.isEmpty()) return null;
        for (String want : preferredTypes) {
            for (XSLFSlideMaster m : masters) {
                XSLFSlideLayout l = findLayout(m, new String[]{want});
                if (l != null) return l;
            }
        }
        // 返回第一个母版的第一个布局作为保底
        try {
            XSLFSlideMaster m0 = masters.get(0);
            XSLFSlideLayout[] arr = m0.getSlideLayouts();
            return (arr != null && arr.length > 0) ? arr[0] : null;
        } catch (Throwable ignore) { return null; }
    }

    private void writeTitleOrFallback(XSLFSlide slide, String title) {
        if (writeTitle(slide, title)) {
            return;
        }
        XSLFTextShape fallback = findFirstTextShape(slide,
                ts -> ts.getTextType() != Placeholder.BODY && ts.getTextType() != Placeholder.TITLE);
        if (fallback != null) {
            fallback.clearText();
            XSLFTextParagraph p = fallback.addNewTextParagraph();
            p.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(title);
            r.setFontSize(36.0);
            r.setBold(true);
        }
    }

    private boolean writeTitle(XSLFSlide slide, String title) {
        XSLFTextShape titleShape = findFirstTextShape(slide, ts -> ts.getTextType() == Placeholder.TITLE);
        if (titleShape != null) {
            titleShape.clearText();
            XSLFTextParagraph p = titleShape.addNewTextParagraph();
            XSLFTextRun r = p.addNewTextRun();
            r.setText(title);
            r.setFontSize(32.0);
            r.setBold(true);
            return true;
        }
        return false;
    }

    private boolean writeBulletsIntoAnyTextShape(XSLFSlide slide, List<String> bullets) {
        XSLFTextShape target = findFirstTextShape(slide,
                ts -> ts.getTextType() == null || (ts.getTextType() != Placeholder.TITLE && ts.getTextType() != Placeholder.SUBTITLE));
        if (target == null) return false;
        fillBullets(target, bullets);
        return true;
    }



    private void writeBullets(XSLFSlide slide, List<String> bullets) {
        // 优先 BODY 占位符
        XSLFTextShape bodyShape = findFirstTextShape(slide, ts -> ts.getTextType() == Placeholder.BODY);
        if (bodyShape != null) {
            fillBullets(bodyShape, bullets);
            return;
        }
        // 回退：不新建形状，写入任意现有文本形状
        writeBulletsIntoAnyTextShape(slide, bullets);
    }

    private XSLFSlide cloneTemplateSlide(XMLSlideShow target, XSLFSlide templateSlide) {
        XSLFSlideLayout layout = matchLayout(target, templateSlide.getSlideLayout());
        XSLFSlide newSlide = (layout != null) ? target.createSlide(layout) : target.createSlide();
        newSlide.importContent(templateSlide);
        return newSlide;
    }

    private XSLFSlideLayout matchLayout(XMLSlideShow target, XSLFSlideLayout templateLayout) {
        if (target == null || templateLayout == null) {
            return null;
        }
        String name = null;
        String type = null;
        try { name = templateLayout.getName(); } catch (Throwable ignore) {}
        try { type = String.valueOf(templateLayout.getType()); } catch (Throwable ignore) {}
        for (XSLFSlideMaster master : target.getSlideMasters()) {
            XSLFSlideLayout[] layouts = master.getSlideLayouts();
            if (layouts == null) continue;
            for (XSLFSlideLayout l : layouts) {
                try {
                    if (name != null && name.equalsIgnoreCase(l.getName())) {
                        return l;
                    }
                } catch (Throwable ignore) {}
                try {
                    if (type != null && type.equalsIgnoreCase(String.valueOf(l.getType()))) {
                        return l;
                    }
                } catch (Throwable ignore) {}
            }
        }
        return null;
    }

    private void applySlideContent(XSLFSlide slide, SlidePlan plan, boolean isCover, String defaultTitle) {
        if (slide == null) return;

        clearAllTextShapes(slide);

        String title = plan != null && plan.title != null && !plan.title.isBlank()
                ? plan.title.trim() : defaultTitle;
        String subtitle = plan != null && plan.subtitle != null && !plan.subtitle.isBlank()
                ? plan.subtitle.trim() : null;
        List<String> bullets = cleanBullets(plan != null ? plan.bullets : null);

        writeTitleOrFallback(slide, title);

        if (isCover) {
            SubtitlePlacement subtitlePlacement = writeSubtitle(slide, subtitle, true);
            if (subtitlePlacement == SubtitlePlacement.NONE) {
                clearSubtitlePlaceholder(slide);
            }
            if (!bullets.isEmpty() && subtitlePlacement != SubtitlePlacement.BODY_OR_OTHER) {
                writeBullets(slide, bullets);
            } else if (bullets.isEmpty() && subtitlePlacement != SubtitlePlacement.BODY_OR_OTHER) {
                clearBodyPlaceholders(slide);
            }
            return;
        }

        SubtitlePlacement subtitlePlacement = SubtitlePlacement.NONE;
        if (subtitle != null) {
            subtitlePlacement = writeSubtitle(slide, subtitle, false);
            if (subtitlePlacement == SubtitlePlacement.NONE) {
                bullets.add(0, subtitle);
            }
        } else {
            clearSubtitlePlaceholder(slide);
        }

        if (!bullets.isEmpty()) {
            writeBullets(slide, bullets);
        } else if (subtitlePlacement != SubtitlePlacement.BODY_OR_OTHER) {
            clearBodyPlaceholders(slide);
        }

        if (subtitlePlacement == SubtitlePlacement.NONE) {
            clearSubtitlePlaceholder(slide);
        }
    }

    private void clearAllTextShapes(XSLFSlide slide) {
        for (XSLFTextShape ts : collectTextShapes(slide)) {
            if (ts.getTextType() != null) {
                ts.clearText();
                continue;
            }
            String existing = ts.getText();
            if (existing == null) {
                continue;
            }
            String normalized = existing.replaceAll("\s+", "");
            String lower = normalized.toLowerCase();
            if (normalized.contains("单击此处") || lower.contains("clicktoedit")) {
                ts.clearText();
            }
        }
    }

    private List<String> cleanBullets(List<String> bullets) {
        if (bullets == null) {
            return new ArrayList<>();
        }
        List<String> cleaned = new ArrayList<>();
        for (String b : bullets) {
            if (b == null) continue;
            String trimmed = b.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }

    private void clearSubtitlePlaceholder(XSLFSlide slide) {
        for (XSLFTextShape ts : collectTextShapes(slide)) {
            if (ts.getTextType() == Placeholder.SUBTITLE) {
                ts.clearText();
            }
        }
    }

    private void clearBodyPlaceholders(XSLFSlide slide) {
        for (XSLFTextShape ts : collectTextShapes(slide)) {
            if (ts.getTextType() == Placeholder.BODY) {
                ts.clearText();
            }
        }
    }

    private SubtitlePlacement writeSubtitle(XSLFSlide slide, String subtitle, boolean allowBodyFallback) {
        if (subtitle == null || subtitle.isBlank()) {
            clearSubtitlePlaceholder(slide);
            return SubtitlePlacement.NONE;
        }
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape ts) {
                if (ts.getTextType() == Placeholder.SUBTITLE) {
                    ts.clearText();
                    XSLFTextParagraph p = ts.addNewTextParagraph();
                    XSLFTextRun r = p.addNewTextRun();
                    r.setText(subtitle);
                    r.setFontSize(20.0);
                    return SubtitlePlacement.SUBTITLE_PLACEHOLDER;
                }
            }
        }
        if (!allowBodyFallback) {
            return SubtitlePlacement.NONE;
        }
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape ts) {
                if (ts.getTextType() == Placeholder.BODY) {
                    ts.clearText();
                    XSLFTextParagraph p = ts.addNewTextParagraph();
                    XSLFTextRun r = p.addNewTextRun();
                    r.setText(subtitle);
                    r.setFontSize(18.0);
                    return SubtitlePlacement.BODY_OR_OTHER;
                }
            }
        }
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape ts) {
                if (ts.getTextType() == null) {
                    ts.clearText();
                    XSLFTextParagraph p = ts.addNewTextParagraph();
                    XSLFTextRun r = p.addNewTextRun();
                    r.setText(subtitle);
                    r.setFontSize(18.0);
                    return SubtitlePlacement.BODY_OR_OTHER;
                }
            }
        }
        return SubtitlePlacement.NONE;
    }

    private List<XSLFTextShape> collectTextShapes(XSLFSlide slide) {
        List<XSLFTextShape> results = new ArrayList<>();
        collectTextShapes(results, slide.getShapes());
        return results;
    }

    private void collectTextShapes(List<XSLFTextShape> collector, List<? extends XSLFShape> shapes) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTextShape ts) {
                collector.add(ts);
            } else if (shape instanceof XSLFGroupShape group) {
                collectTextShapes(collector, group.getShapes());
            }
        }
    }

    private XSLFTextShape findFirstTextShape(XSLFSlide slide, Predicate<XSLFTextShape> predicate) {
        for (XSLFTextShape ts : collectTextShapes(slide)) {
            if (predicate.test(ts)) {
                return ts;
            }
        }
        return null;
    }

    private void fillBullets(XSLFTextShape target, List<String> bullets) {
        target.clearText();
        for (String b : bullets) {
            if (b == null) continue;
            String txt = b.trim();
            if (txt.isEmpty()) continue;
            txt = txt.replaceAll("^[\u2022\u00B70-9. \t-]+", "");
            XSLFTextParagraph paragraph = target.addNewTextParagraph();
            paragraph.setBullet(true);
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(txt);
            run.setFontSize(20.0);
        }
    }

    private void collectPlaceholderDescriptors(Map<String, PlaceholderDescriptor> collector, XSLFShape shape, int slideIndex) {
        if (shape instanceof XSLFGroupShape group) {
            for (XSLFShape child : group.getShapes()) {
                collectPlaceholderDescriptors(collector, child, slideIndex);
            }
            return;
        }
        if (!(shape instanceof XSLFTextShape ts)) {
            return;
        }
        String raw = ts.getText();
        if (raw == null) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return;
        }
        boolean bulletStyle = false;
        for (XSLFTextParagraph paragraph : ts.getTextParagraphs()) {
            if (paragraph.isBullet()) {
                bulletStyle = true;
                break;
            }
        }
        matcher.reset();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name == null) continue;
            name = name.trim();
            if (name.isEmpty()) continue;
            PlaceholderDescriptor descriptor = collector.get(name);
            boolean listCandidate = bulletStyle || looksLikeListName(name);
            String sample = summarizeSampleText(raw);
            if (descriptor == null) {
                descriptor = new PlaceholderDescriptor();
                descriptor.name = name;
                descriptor.slideIndex = slideIndex;
                descriptor.list = listCandidate;
                descriptor.sampleText = sample;
                collector.put(name, descriptor);
            } else {
                if (!descriptor.list && listCandidate) {
                    descriptor.list = true;
                }
                if (slideIndex < descriptor.slideIndex) {
                    descriptor.slideIndex = slideIndex;
                }
                if ((descriptor.sampleText == null || descriptor.sampleText.isBlank()) && sample != null && !sample.isBlank()) {
                    descriptor.sampleText = sample;
                }
            }
        }
    }

    private boolean looksLikeListName(String name) {
        String lower = name.toLowerCase();
        return lower.contains("list") || lower.contains("bullet") || lower.contains("points") || lower.endsWith("items") || lower.endsWith("lines") || lower.contains("outline");
    }

    private String summarizeSampleText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = PLACEHOLDER_PATTERN.matcher(text).replaceAll("").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.length() > 40) {
            cleaned = cleaned.substring(0, 40) + "...";
        }
        return cleaned;
    }

    private Map<String, PlaceholderValue> parsePlaceholderValues(String content, TemplateInfo templateInfo) {
        Map<String, PlaceholderValue> values = new LinkedHashMap<>();
        if (content == null) {
            return values;
        }
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return values;
            }
            String json = content.substring(start, end + 1);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode node = root.has("placeholders") ? root.get("placeholders") : root;
            if (node == null || !node.isObject()) {
                return values;
            }
            Map<String, PlaceholderDescriptor> descriptorMap = new LinkedHashMap<>();
            if (templateInfo.placeholders != null) {
                for (PlaceholderDescriptor descriptor : templateInfo.placeholders) {
                    descriptorMap.putIfAbsent(descriptor.name, descriptor);
                }
            }
            node.fieldNames().forEachRemaining(key -> {
                PlaceholderDescriptor descriptor = descriptorMap.get(key);
                JsonNode valueNode = node.get(key);
                if (descriptor == null || valueNode == null || valueNode.isNull()) {
                    return;
                }
                if (descriptor.list) {
                    List<String> items = extractListFromNode(valueNode);
                    if (!items.isEmpty()) {
                        values.put(key, PlaceholderValue.listValue(items));
                    }
                } else {
                    String textValue = extractTextFromNode(valueNode);
                    if (textValue != null && !textValue.isBlank()) {
                        values.put(key, PlaceholderValue.textValue(textValue.trim()));
                    }
                }
            });
        } catch (Exception e) {
            log.warn("占位符 JSON 解析失败: {}", e.getMessage());
        }
        return values;
    }

    private List<String> extractListFromNode(JsonNode node) {
        List<String> items = new ArrayList<>();
        if (node == null || node.isNull()) {
            return items;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String text = child.asText(null);
                if (text == null) continue;
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    items.add(trimmed);
                }
            }
        } else if (node.isTextual()) {
            String text = node.asText();
            for (String part : text.split("[\n；;、]+")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    items.add(trimmed);
                }
            }
        }
        return items;
    }

    private String extractTextFromNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode child : node) {
                String text = child.asText(null);
                if (text == null) continue;
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    parts.add(trimmed);
                }
            }
            return String.join("；", parts);
        }
        return node.asText();
    }

    private void applyPlaceholderValues(XMLSlideShow ppt, Map<String, PlaceholderValue> values, TemplateInfo templateInfo) {
        if (ppt == null) {
            return;
        }
        for (XSLFSlide slide : ppt.getSlides()) {
            applyPlaceholderValuesToShapeList(slide.getShapes(), values);
        }
        removeResidualPlaceholders(ppt);
    }

    private void applyPlaceholderValuesToShapeList(List<? extends XSLFShape> shapes, Map<String, PlaceholderValue> values) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape group) {
                applyPlaceholderValuesToShapeList(group.getShapes(), values);
            } else if (shape instanceof XSLFTextShape ts) {
                replacePlaceholdersInTextShape(ts, values);
            }
        }
    }

    private void replacePlaceholdersInTextShape(XSLFTextShape textShape, Map<String, PlaceholderValue> values) {
        String raw = textShape.getText();
        if (raw == null) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        if (matches.isEmpty()) {
            return;
        }

        if (matches.size() == 1) {
            String name = matches.get(0).trim();
            PlaceholderValue value = values.get(name);
            if (value == null) {
                clearPlaceholderText(textShape, name);
                return;
            }
            String token = "{{" + name + "}}";
            if (isExactPlaceholder(textShape, token) && value.isList()) {
                fillBullets(textShape, value.normalizedItems());
                return;
            }
            if (isExactPlaceholder(textShape, token)) {
                setShapeText(textShape, value.asSingleLine());
                return;
            }
        }

        // 部分占位符替换：保持原有段落结构
        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
            List<XSLFTextRun> runs = paragraph.getTextRuns();
            for (XSLFTextRun run : runs) {
                String text = run.getRawText();
                if (text == null) continue;
                Matcher runMatcher = PLACEHOLDER_PATTERN.matcher(text);
                StringBuffer buffer = new StringBuffer();
                boolean replaced = false;
                while (runMatcher.find()) {
                    String name = runMatcher.group(1).trim();
                    PlaceholderValue value = values.get(name);
                    String replacement = value != null ? value.asSingleLine() : "";
                    runMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                    replaced = true;
                }
                if (replaced) {
                    runMatcher.appendTail(buffer);
                    run.setText(buffer.toString());
                }
            }
        }
    }

    private void clearPlaceholderText(XSLFTextShape textShape, String placeholderName) {
        String token = "{{" + placeholderName + "}}";
        if (isExactPlaceholder(textShape, token)) {
            textShape.clearText();
            return;
        }
        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                String text = run.getRawText();
                if (text != null && text.contains(token)) {
                    run.setText(text.replace(token, ""));
                }
            }
        }
    }

    private boolean isExactPlaceholder(XSLFTextShape textShape, String token) {
        String raw = textShape.getText();
        if (raw == null) {
            return false;
        }
        String normalized = raw.replace("•", "").replace("\u2022", "").replace("\u00B7", "");
        normalized = normalized.replaceAll("\\s+", "");
        String tokenNormalized = token.replaceAll("\\s+", "");
        return normalized.equals(tokenNormalized);
    }

    private void setShapeText(XSLFTextShape textShape, String value) {
        textShape.clearText();
        if (value == null || value.isBlank()) {
            return;
        }
        XSLFTextParagraph paragraph = textShape.addNewTextParagraph();
        paragraph.setBullet(false);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(value.trim());
        run.setFontSize(24.0);
    }

    private void removeResidualPlaceholders(XMLSlideShow ppt) {
        for (XSLFSlide slide : ppt.getSlides()) {
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFGroupShape group) {
                    removeResidualPlaceholdersFromGroup(group);
                } else if (shape instanceof XSLFTextShape ts) {
                    stripPlaceholders(ts);
                }
            }
        }
    }

    private void removeResidualPlaceholdersFromGroup(XSLFGroupShape group) {
        for (XSLFShape shape : group.getShapes()) {
            if (shape instanceof XSLFGroupShape subGroup) {
                removeResidualPlaceholdersFromGroup(subGroup);
            } else if (shape instanceof XSLFTextShape ts) {
                stripPlaceholders(ts);
            }
        }
    }

    private void stripPlaceholders(XSLFTextShape textShape) {
        String raw = textShape.getText();
        if (raw == null) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return;
        }
        boolean modified = false;
        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                String text = run.getRawText();
                if (text == null) {
                    continue;
                }
                Matcher runMatcher = PLACEHOLDER_PATTERN.matcher(text);
                if (runMatcher.find()) {
                    String replaced = runMatcher.replaceAll("");
                    run.setText(replaced);
                    modified = true;
                }
            }
        }
        if (modified) {
            String updated = textShape.getText();
            if (updated == null || updated.trim().isEmpty()) {
                textShape.clearText();
            }
        }
    }

    private PptPlan buildFallbackPlan(PptGeneration pg, String content) {
        PptPlan plan = new PptPlan();
        plan.slides = new ArrayList<>();

        SlidePlan cover = new SlidePlan();
        cover.layout = "TITLE";
        cover.title = pg.getTitle();
        plan.slides.add(cover);

        String[] lines = content.split("\r?\n");
        StringBuilder buf = new StringBuilder();
        List<String> sections = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (buf.length() > 0) {
                    sections.add(buf.toString());
                    buf.setLength(0);
                }
            } else {
                buf.append(line).append('\n');
            }
        }
        if (buf.length() > 0) {
            sections.add(buf.toString());
        }
        if (sections.isEmpty()) {
            sections.add("目录\n• 背景\n• 方案\n• 展望");
        }

        int desiredSlides = pg.getSlideCount() != null && pg.getSlideCount() > 0 ? Math.min(pg.getSlideCount(), 30) : 10;
        int count = Math.min(sections.size(), Math.max(desiredSlides - 1, 1));
        for (int i = 0; i < count; i++) {
            String sec = sections.get(i);
            String[] secLines = sec.split("\r?\n");
            SlidePlan sp = new SlidePlan();
            sp.layout = "TITLE_AND_CONTENT";
            sp.title = secLines.length > 0 ? secLines[0].trim() : ("第" + (i + 1) + "页");
            if (sp.title == null || sp.title.isBlank()) {
                sp.title = "第" + (i + 1) + "页";
            }
            sp.bullets = new ArrayList<>();
            for (int j = 1; j < secLines.length; j++) {
                String txt = secLines[j].trim();
                if (!txt.isEmpty()) {
                    sp.bullets.add(txt.replaceAll("^[\u2022\u00B70-9. \t-]+", ""));
                }
            }
            plan.slides.add(sp);
        }

        return plan;
    }

    // ---- 模板检查 & AI 计划解析 ----
    private TemplateInfo inspectTemplate(byte[] templateBytes) {
        if (templateBytes == null || templateBytes.length == 0) return null;
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(templateBytes))) {
            TemplateInfo info = new TemplateInfo();
            Dimension d = show.getPageSize();
            info.pageWidth = d != null ? d.width : 1280;
            info.pageHeight = d != null ? d.height : 720;
            info.layouts = new ArrayList<>();
            Map<String, PlaceholderDescriptor> placeholderMap = new LinkedHashMap<>();
            for (XSLFSlideMaster m : show.getSlideMasters()) {
                XSLFSlideLayout[] arr = m.getSlideLayouts();
                if (arr == null) continue;
                for (XSLFSlideLayout l : arr) {
                    LayoutInfo li = new LayoutInfo();
                    try { li.name = l.getName(); } catch (Throwable ignore) {}
                    try { li.typeName = String.valueOf(l.getType()); } catch (Throwable ignore) {}
                    li.placeholders = new ArrayList<>();
                    try {
                        for (XSLFShape s : l.getShapes()) {
                            if (s instanceof XSLFTextShape ts) {
                                Placeholder pt = ts.getTextType();
                                if (pt != null) li.placeholders.add(String.valueOf(pt));
                            }
                        }
                    } catch (Throwable ignore) {}
                    info.layouts.add(li);
                }
            }
            info.samples = new ArrayList<>();
            int maxSlides = Math.min(8, show.getSlides().size());
            for (int idx = 0; idx < maxSlides; idx++) {
                XSLFSlide slide = show.getSlides().get(idx);
                SlideSample sample = new SlideSample();
                XSLFSlideLayout layout = slide.getSlideLayout();
                if (layout != null) {
                    try { sample.layoutName = layout.getName(); } catch (Throwable ignore) {}
                    try { sample.typeName = String.valueOf(layout.getType()); } catch (Throwable ignore) {}
                }
                sample.texts = new ArrayList<>();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape ts) {
                        SampleText st = new SampleText();
                        Placeholder pt = ts.getTextType();
                        if (pt != null) {
                            st.placeholderType = String.valueOf(pt);
                        }
                        st.text = ts.getText();
                        sample.texts.add(st);
                    }
                    collectPlaceholderDescriptors(placeholderMap, shape, idx);
                }
                info.samples.add(sample);
            }
            info.placeholders = new ArrayList<>(placeholderMap.values());
            return info;
        } catch (IOException e) {
            log.warn("模板检查失败: {}", e.getMessage());
            return null;
        }
    }

    private PptPlan parsePlan(String content) {
        if (content == null) return null;
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) return null;
            String json = content.substring(start, end + 1);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode slides = root.get("slides");
            if (slides == null || !slides.isArray()) return null;
            PptPlan plan = new PptPlan();
            plan.slides = new ArrayList<>();
            for (JsonNode n : slides) {
                SlidePlan sp = new SlidePlan();
                sp.layout = textOf(n.get("layout"));
                sp.title = textOf(n.get("title"));
                sp.subtitle = textOf(n.get("subtitle"));
                sp.bullets = new ArrayList<>();
                JsonNode bullets = n.get("bullets");
                if (bullets == null) bullets = n.get("points");
                if (bullets != null && bullets.isArray()) {
                    for (JsonNode b : bullets) sp.bullets.add(textOf(b));
                }
                plan.slides.add(sp);
            }
            return plan;
        } catch (Exception e) {
            log.info("AI 返回非 JSON 结构，使用传统分段方式: {}", e.getMessage());
            return null;
        }
    }

    private String textOf(JsonNode n) { return n == null || n.isNull() ? null : n.asText(); }

    // ---- 简单数据结构 ----
    private static class TemplateInfo {
        int pageWidth;
        int pageHeight;
        List<LayoutInfo> layouts;
        List<SlideSample> samples;
        List<PlaceholderDescriptor> placeholders;

        boolean hasPlaceholders() {
            return placeholders != null && !placeholders.isEmpty();
        }
    }

    private static class LayoutInfo { String name; String typeName; List<String> placeholders; }

    private static class SlideSample {
        String layoutName;
        String typeName;
        List<SampleText> texts;
    }

    private static class SampleText {
        String placeholderType;
        String text;
    }

    private static class PlaceholderDescriptor {
        String name;
        int slideIndex;
        boolean list;
        String sampleText;
    }

    private static class PptPlan { List<SlidePlan> slides; }

    private static class SlidePlan { String layout; String title; String subtitle; List<String> bullets; }

    private enum SubtitlePlacement { NONE, SUBTITLE_PLACEHOLDER, BODY_OR_OTHER }

    private record PlaceholderValue(boolean list, String text, List<String> items) {
        static PlaceholderValue textValue(String value) { return new PlaceholderValue(false, value, Collections.emptyList()); }
        static PlaceholderValue listValue(List<String> items) { return new PlaceholderValue(true, null, items); }
        boolean isList() { return list; }
        List<String> normalizedItems() { return items == null ? Collections.emptyList() : items; }
        String asSingleLine() {
            if (!isList()) {
                return text != null ? text : "";
            }
            return String.join("；", normalizedItems());
        }
    }

    private record GeneratedPptFiles(byte[] pptxBytes, byte[] thumbBytes) {}
    
    @Override
    public PptGenerationResponse getGenerationStatus(Long userId, Long id) {
        PptGeneration pptGeneration = pptGenerationMapper.selectById(id);
        if (pptGeneration == null || !pptGeneration.getUserId().equals(userId)) {
            throw new BusinessException("PPT生成记录不存在");
        }
        
        return convertToResponse(pptGeneration);
    }
    
    @Override
    public List<PptGenerationResponse> getUserGenerationHistory(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<PptGeneration> pptGenerations = pptGenerationMapper.selectByUserId(userId, offset, size);
        
        return pptGenerations.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteGeneration(Long userId, Long id) {
        PptGeneration pptGeneration = pptGenerationMapper.selectById(id);
        if (pptGeneration == null || !pptGeneration.getUserId().equals(userId)) {
            throw new BusinessException("PPT生成记录不存在");
        }
        
        pptGenerationMapper.deleteById(id);
        log.info("删除PPT生成记录成功，用户ID: {}, 记录ID: {}", userId, id);
    }
    
    @Override
    public List<PptTemplateResponse> getAvailableTemplates() {
        List<PptTemplate> templates = pptTemplateMapper.selectEnabled();
        
        return templates.stream()
                .map(this::convertTemplateToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PptTemplateResponse> getTemplatesByCategory(String category) {
        List<PptTemplate> templates = pptTemplateMapper.selectByCategory(category);
        
        return templates.stream()
                .map(this::convertTemplateToResponse)
                .collect(Collectors.toList());
    }
    
    private PptGenerationResponse convertToResponse(PptGeneration pptGeneration) {
        PptGenerationResponse response = new PptGenerationResponse();
        BeanUtils.copyProperties(pptGeneration, response);
        
        // 设置状态描述
        switch (pptGeneration.getStatus()) {
            case 0:
                response.setStatusDesc("生成中");
                break;
            case 1:
                response.setStatusDesc("生成成功");
                break;
            case 2:
                response.setStatusDesc("生成失败");
                break;
            default:
                response.setStatusDesc("未知状态");
        }
        
        return response;
    }
    
    private PptTemplateResponse convertTemplateToResponse(PptTemplate pptTemplate) {
        PptTemplateResponse response = new PptTemplateResponse();
        BeanUtils.copyProperties(pptTemplate, response);
        return response;
    }
}