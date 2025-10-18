package com.kmu.edu.back_service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kmu.edu.back_service.common.ResultCode;
import com.kmu.edu.back_service.constant.Constants;
import com.kmu.edu.back_service.dto.request.VideoGenerationRequest;
import com.kmu.edu.back_service.dto.response.VideoGenerationResponse;
import com.kmu.edu.back_service.entity.VideoGeneration;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.mapper.VideoGenerationMapper;
import com.kmu.edu.back_service.service.SiliconAIService;
import com.kmu.edu.back_service.service.SysUserService;
import com.kmu.edu.back_service.service.VideoGenerationService;
import com.kmu.edu.back_service.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private final VideoGenerationMapper videoGenerationMapper;
    private final SysUserService userService;
    private final SiliconAIService siliconAIService;
    private final com.kmu.edu.back_service.mapper.UserUsageRecordMapper userUsageRecordMapper;
    private final com.kmu.edu.back_service.service.MinioStorageService minioStorageService;

    @Override
    @Transactional
    public VideoGenerationResponse generateVideo(VideoGenerationRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        userService.checkDailyLimit(userId, Constants.ServiceType.VIDEO);
        if (!userService.useTokens(userId, Constants.TokenCost.VIDEO)) {
            throw new BusinessException(ResultCode.INSUFFICIENT_TOKENS);
        }

        try {
            // 创建生成记录
            VideoGeneration record = new VideoGeneration();
            record.setUserId(userId);
            record.setPrompt(request.getPrompt());
            record.setSourceType(request.getSourceType());
            record.setSourceImageUrl(request.getSourceImageUrl());
            record.setDuration(request.getDuration());
            record.setStyle(request.getStyle());
            record.setStatus(Constants.GenerationStatus.GENERATING);
            record.setProgress(0);
            videoGenerationMapper.insert(record);

            // 异步处理
            processGenerationTask(record.getId());

            VideoGenerationResponse resp = new VideoGenerationResponse();
            resp.setId(record.getId());
            resp.setStatus(Constants.GenerationStatus.GENERATING);
            resp.setProgress(0);
            resp.setCreateTime(LocalDateTime.now());
            return resp;

        } catch (Exception e) {
            userService.addTokens(userId, Constants.TokenCost.VIDEO);
            log.error("视频生成任务创建失败: userId={}, err= {}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.GENERATION_FAILED, "视频生成失败，请稍后重试");
        }
    }

    @Override
    public VideoGenerationResponse getGenerationStatus(Long id) {
        VideoGeneration r = videoGenerationMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "生成记录不存在");

        Long userId = StpUtil.getLoginIdAsLong();
        if (!r.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);

        VideoGenerationResponse resp = new VideoGenerationResponse();
        resp.setId(r.getId());
        resp.setStatus(r.getStatus());
        resp.setProgress(r.getProgress());
        resp.setVideoUrl(r.getVideoUrl());
        resp.setThumbnailUrl(r.getThumbnailUrl());
        resp.setGenerationTime(r.getGenerationTime());
        resp.setErrorMessage(r.getErrorMessage());
        resp.setCreateTime(r.getCreateTime());
        return resp;
    }

    @Override
    public List<VideoGeneration> getUserGenerations(Long userId, Integer page, Integer size) {
        int offset = CommonUtils.calculateOffset(page, size);
        return videoGenerationMapper.selectByUserId(userId, offset, size);
    }

    @Override
    @Transactional
    public void deleteGeneration(Long id) {
        VideoGeneration r = videoGenerationMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "生成记录不存在");

        Long userId = StpUtil.getLoginIdAsLong();
        if (!r.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);

        videoGenerationMapper.deleteById(id);
        log.info("删除视频生成记录: userId={}, id={}", userId, id);
    }

    @Override
    @Async
    public void processGenerationTask(Long id) {
        try {
            VideoGeneration r = videoGenerationMapper.selectById(id);
            if (r == null) return;

            String videoUrl = siliconAIService.generateVideo(
                    r.getPrompt(),
                    r.getSourceType() != null && r.getSourceType() == Constants.VideoSourceType.IMAGE ? r.getSourceImageUrl() : null
            );

            // 下载并转存到 MinIO
            String minioUrl = null;
            try {
                java.net.URL url = java.net.URI.create(videoUrl).toURL();
                try (java.io.InputStream in = url.openStream()) {
                    // 生成对象名：video/{userId}/{recordId}.mp4（仅示例，真实可加时间戳/随机）
                    String objectName = String.format("video/%d/%d.mp4", r.getUserId(), r.getId());
                    minioUrl = minioStorageService.upload(objectName, in, -1, "video/mp4");
                }
            } catch (Exception ex) {
                log.warn("视频转存MinIO失败，退回使用直链: id={}, src={}, err={}", id, videoUrl, ex.getMessage());
            }

            r.setVideoUrl(minioUrl != null ? minioUrl : videoUrl);
            r.setThumbnailUrl(null);
            r.setStatus(Constants.GenerationStatus.SUCCESS);
            r.setProgress(100);
            r.setGenerationTime(20);
            videoGenerationMapper.updateResult(r);
            log.info("视频生成完成: id={}", id);

            // 使用记录
            com.kmu.edu.back_service.entity.UserUsageRecord rec = new com.kmu.edu.back_service.entity.UserUsageRecord();
            rec.setUserId(r.getUserId());
            rec.setServiceType(Constants.ServiceType.VIDEO);
            rec.setTokensUsed(Constants.TokenCost.VIDEO);
            rec.setRequestContent(r.getPrompt());
            rec.setResponseContent(videoUrl);
            rec.setStatus(1);
            try { userUsageRecordMapper.insert(rec); } catch (Exception ignore) {}

        } catch (Exception e) {
            log.error("视频生成失败: id={}", id, e);
            VideoGeneration r = videoGenerationMapper.selectById(id);
            if (r != null) {
                r.setStatus(Constants.GenerationStatus.FAILED);
                String msg = e.getMessage();
                if (msg != null && msg.length() > 500) msg = msg.substring(0, 500);
                r.setErrorMessage(msg != null ? msg : "生成失败");
                videoGenerationMapper.updateResult(r);
                // 使用记录（失败）
                com.kmu.edu.back_service.entity.UserUsageRecord rec = new com.kmu.edu.back_service.entity.UserUsageRecord();
                rec.setUserId(r.getUserId());
                rec.setServiceType(Constants.ServiceType.VIDEO);
                rec.setTokensUsed(0);
                rec.setRequestContent(r.getPrompt());
                rec.setResponseContent("生成失败");
                rec.setStatus(0);
                try { userUsageRecordMapper.insert(rec); } catch (Exception ignore) {}
            }
        }
    }
}
