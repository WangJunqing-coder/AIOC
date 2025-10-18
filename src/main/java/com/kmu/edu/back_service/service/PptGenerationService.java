package com.kmu.edu.back_service.service;

import com.kmu.edu.back_service.dto.request.PptGenerationRequest;
import com.kmu.edu.back_service.dto.response.PptGenerationResponse;
import com.kmu.edu.back_service.dto.response.PptTemplateResponse;

import java.util.List;

/**
 * PPT生成服务接口
 */
public interface PptGenerationService {
    
    /**
     * 生成PPT
     * 
     * @param userId 用户ID
     * @param request 生成请求
     * @return 生成结果
     */
    PptGenerationResponse generatePpt(Long userId, PptGenerationRequest request);
    
    /**
     * 查询PPT生成状态
     * 
     * @param userId 用户ID
     * @param id 生成记录ID
     * @return 生成状态
     */
    PptGenerationResponse getGenerationStatus(Long userId, Long id);
    
    /**
     * 获取用户PPT生成历史
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 生成历史列表
     */
    List<PptGenerationResponse> getUserGenerationHistory(Long userId, Integer page, Integer size);
    
    /**
     * 删除PPT生成记录
     * 
     * @param userId 用户ID
     * @param id 生成记录ID
     */
    void deleteGeneration(Long userId, Long id);
    
    /**
     * 获取可用的PPT模板列表
     * 
     * @return 模板列表
     */
    List<PptTemplateResponse> getAvailableTemplates();
    
    /**
     * 根据分类获取PPT模板
     * 
     * @param category 分类
     * @return 模板列表
     */
    List<PptTemplateResponse> getTemplatesByCategory(String category);
}