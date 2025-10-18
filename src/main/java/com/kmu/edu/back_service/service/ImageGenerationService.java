package com.kmu.edu.back_service.service;

import com.github.pagehelper.PageInfo;
import com.kmu.edu.back_service.dto.request.ImageGenerationRequest;
import com.kmu.edu.back_service.dto.response.ImageGenerationResponse;
import com.kmu.edu.back_service.entity.ImageGeneration;


/**
 * 图片生成服务接口
 */
public interface ImageGenerationService {
    
    /**
     * 生成图片
     */
    ImageGenerationResponse generateImage(ImageGenerationRequest request);
    
    /**
     * 查询生成进度
     */
    ImageGenerationResponse getGenerationStatus(Long id);
    
    /**
     * 获取用户生成历史
     */
    PageInfo<ImageGeneration> getUserGenerations(Long userId, Integer page, Integer size);
    
    /**
     * 删除生成记录
     */
    void deleteGeneration(Long id);
    
    /**
     * 处理生成任务
     */
    void processGenerationTask(Long id);
}