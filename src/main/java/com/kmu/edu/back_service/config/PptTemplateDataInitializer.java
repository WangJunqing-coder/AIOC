package com.kmu.edu.back_service.config;

import com.kmu.edu.back_service.entity.PptTemplate;
import com.kmu.edu.back_service.mapper.PptTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 在应用启动时为 ppt_template 表注入一些内置模板，避免前端“没有模板”的空状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PptTemplateDataInitializer implements CommandLineRunner {

    private final PptTemplateMapper pptTemplateMapper;

    @Override
    public void run(String... args) {
        try {
            Long total = pptTemplateMapper.countAll();
            if (total == null || total == 0L) {
                log.info("PPT 模板表为空，注入内置模板...");
                insertTemplate("商务简约", "适用于商务汇报/项目路演的简洁风格", "builtin://business-simple", null, "business", 10, 1);
                insertTemplate("科技蓝", "科技/互联网主题，蓝色系配色", "builtin://tech-blue", null, "tech", 20, 1);
                insertTemplate("教育教学", "教学课件模板，结构清晰，易读可展示", "builtin://education", null, "education", 30, 1);
                log.info("内置模板注入完成");
            }
        } catch (Exception e) {
            log.warn("初始化 PPT 模板失败（忽略，不影响应用启动）：{}", e.getMessage());
        }
    }

    private void insertTemplate(String name, String desc, String url, String thumb, String category, int sort, int status) {
        PptTemplate t = new PptTemplate();
        t.setTemplateName(name);
        t.setTemplateDesc(desc);
        t.setTemplateUrl(url); // 表结构非空，使用内置标识
        t.setThumbnailUrl(thumb);
        t.setCategory(category);
        t.setSortOrder(sort);
        t.setStatus(status);
        pptTemplateMapper.insert(t);
    }
}
