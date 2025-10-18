package com.kmu.edu.back_service.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 图片生成记录实体
 */
@Data
public class ImageGeneration {
    
    /**
     * 生成ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 生成提示词
     */
    private String prompt;
    
    /**
     * 图片风格
     */
    private String style;
    
    /**
     * 图片尺寸
     */
    private String size;
    
    /**
     * 图片URL
     */
    private String imageUrl;
    
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