package com.kmu.edu.back_service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kmu.edu.back_service.common.ResultCode;
import com.kmu.edu.back_service.constant.Constants;
import com.kmu.edu.back_service.dto.request.ImageGenerationRequest;
import com.kmu.edu.back_service.dto.response.ImageGenerationResponse;
import com.kmu.edu.back_service.entity.ImageGeneration;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.mapper.ImageGenerationMapper;
import com.kmu.edu.back_service.service.ImageGenerationService;
import com.kmu.edu.back_service.service.SiliconAIService;
import com.kmu.edu.back_service.service.SysUserService;
import com.kmu.edu.back_service.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * 图片生成服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationServiceImpl implements ImageGenerationService {
    
    private final ImageGenerationMapper imageGenerationMapper;
    private final SysUserService userService;
    private final SiliconAIService siliconAIService;
    private final com.kmu.edu.back_service.mapper.UserUsageRecordMapper userUsageRecordMapper;
    private final com.kmu.edu.back_service.service.storage.MinioStorageService minioStorageService;
    
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;
    
    @Override
    @Transactional
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 检查每日使用限制
        userService.checkDailyLimit(userId, Constants.ServiceType.IMAGE);
        
        // 检查并扣减tokens
        if (!userService.useTokens(userId, Constants.TokenCost.IMAGE)) {
            throw new BusinessException(ResultCode.INSUFFICIENT_TOKENS);
        }
        
        try {
            // 创建生成记录
            ImageGeneration record = new ImageGeneration();
            record.setUserId(userId);
            record.setPrompt(request.getPrompt());
            record.setStyle(request.getStyle());
            record.setSize(request.getSize());
            record.setStatus(Constants.GenerationStatus.GENERATING);
            
            imageGenerationMapper.insert(record);
            
            // 异步处理生成任务（生成后会把外部URL转存至MinIO）
            processGenerationTask(record.getId());
            
            // 构建响应
            ImageGenerationResponse response = new ImageGenerationResponse();
            response.setId(record.getId());
            response.setStatus(Constants.GenerationStatus.GENERATING);
            response.setCreateTime(LocalDateTime.now());
            
            log.info("图片生成任务已创建：用户ID={}, 任务ID={}", userId, record.getId());
            return response;
            
        } catch (Exception e) {
            // 如果出错，退还tokens
            userService.addTokens(userId, Constants.TokenCost.IMAGE);
            log.error("图片生成失败：用户ID={}, 错误信息={}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.GENERATION_FAILED, "图片生成失败，请稍后重试");
        }
    }
    
    @Override
    public ImageGenerationResponse getGenerationStatus(Long id) {
        ImageGeneration record = imageGenerationMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "生成记录不存在");
        }
        
        // 验证所有权
        Long userId = StpUtil.getLoginIdAsLong();
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        // 构建响应
        ImageGenerationResponse response = new ImageGenerationResponse();
        response.setId(record.getId());
        response.setStatus(record.getStatus());
        response.setGenerationTime(record.getGenerationTime());
        response.setErrorMessage(record.getErrorMessage());
        response.setCreateTime(record.getCreateTime());
        
        if (record.getStatus() == Constants.GenerationStatus.SUCCESS) {
            List<String> imageUrls = new ArrayList<>();
            if (CommonUtils.isNotEmpty(record.getImageUrl())) {
                imageUrls.add(record.getImageUrl());
            }
            response.setImageUrls(imageUrls);
        }
        
        return response;
    }
    
    @Override
    public PageInfo<ImageGeneration> getUserGenerations(Long userId, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<ImageGeneration> list = imageGenerationMapper.selectByUserIdPaged(userId);
        return PageInfo.of(list);
    }
    
    @Override
    @Transactional
    public void deleteGeneration(Long id) {
        ImageGeneration record = imageGenerationMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "生成记录不存在");
        }
        
        // 验证所有权
        Long userId = StpUtil.getLoginIdAsLong();
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        imageGenerationMapper.deleteById(id);
        log.info("删除图片生成记录：用户ID={}, 记录ID={}", userId, id);
    }
    
    @Override
    @Async
    public void processGenerationTask(Long id) {
        try {
            // 使用硅基流动AI服务生成图片
            ImageGeneration record = imageGenerationMapper.selectById(id);
            if (record != null) {
                String srcUrl = siliconAIService.generateImage(
                    record.getPrompt(), 
                    record.getStyle(), 
                    record.getSize()
                );
                // 将外部临时URL保存到 MinIO，返回长期可用URL
                String imageUrl;
                try {
                    imageUrl = minioStorageService.uploadImageFromUrl(srcUrl);
                } catch (Exception ex) {
                    log.warn("上传到 MinIO 失败，回退使用源URL: {}", ex.getMessage());
                    imageUrl = srcUrl; // 回退
                }

                // 更新生成结果
                record.setImageUrl(imageUrl);
                record.setStatus(Constants.GenerationStatus.SUCCESS);
                record.setGenerationTime(5);
                imageGenerationMapper.updateResult(record);
                
                log.info("图片生成完成：ID={}", id);
                // 使用记录
                com.kmu.edu.back_service.entity.UserUsageRecord rec = new com.kmu.edu.back_service.entity.UserUsageRecord();
                rec.setUserId(record.getUserId());
                rec.setServiceType(Constants.ServiceType.IMAGE);
                rec.setTokensUsed(Constants.TokenCost.IMAGE);
                rec.setRequestContent(record.getPrompt());
                rec.setResponseContent(imageUrl);
                rec.setStatus(1);
                try { userUsageRecordMapper.insert(rec); } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            log.error("图片生成失败：ID={}", id, e);
            
            ImageGeneration record = imageGenerationMapper.selectById(id);
            if (record != null) {
                record.setStatus(Constants.GenerationStatus.FAILED);
                record.setErrorMessage("生成失败");
                imageGenerationMapper.updateResult(record);
                // 使用记录（失败）
                com.kmu.edu.back_service.entity.UserUsageRecord rec = new com.kmu.edu.back_service.entity.UserUsageRecord();
                rec.setUserId(record.getUserId());
                rec.setServiceType(Constants.ServiceType.IMAGE);
                rec.setTokensUsed(0);
                rec.setRequestContent(record.getPrompt());
                rec.setResponseContent("生成失败");
                rec.setStatus(0);
                try { userUsageRecordMapper.insert(rec); } catch (Exception ignore) {}
            }
        }
    }
}