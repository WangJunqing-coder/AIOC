package com.kmu.edu.back_service.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * PPT生成记录实体
 */
@Data
public class PptGeneration {
    
    /**
     * 生成ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * PPT标题
     */
    private String title;
    
    /**
     * 生成提示词
     */
    private String prompt;
    
    /**
     * 模板ID
     */
    private String templateId;
    
    /**
     * 幻灯片数量
     */
    private Integer slideCount;
    
    /**
     * PPT文件URL
     */
    private String pptUrl;
    
    /**
     * PDF文件URL
     */
    private String pdfUrl;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 生成耗时(秒)
     */
    private Integer generationTime;
    
    /**
     * 状态:0生成中,1成功,2失败
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}