package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PPT模板响应
 */
@Data
@Schema(description = "PPT模板响应")
public class PptTemplateResponse {
    
    @Schema(description = "模板ID")
    private Long id;
    
    @Schema(description = "模板名称")
    private String templateName;
    
    @Schema(description = "模板描述")
    private String templateDesc;
    
    @Schema(description = "模板文件URL")
    private String templateUrl;
    
    @Schema(description = "缩略图URL")
    private String thumbnailUrl;
    
    @Schema(description = "分类")
    private String category;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态:0禁用,1启用")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}