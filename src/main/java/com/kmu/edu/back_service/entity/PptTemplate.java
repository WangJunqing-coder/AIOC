package com.kmu.edu.back_service.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * PPT模板实体
 */
@Data
public class PptTemplate {
    
    /**
     * 模板ID
     */
    private Long id;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板描述
     */
    private String templateDesc;
    
    /**
     * 模板文件URL
     */
    private String templateUrl;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态:0禁用,1启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}