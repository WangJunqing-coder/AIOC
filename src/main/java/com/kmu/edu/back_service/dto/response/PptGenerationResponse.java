package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PPT生成响应
 */
@Data
@Schema(description = "PPT生成响应")
public class PptGenerationResponse {
    
    @Schema(description = "生成ID")
    private Long id;
    
    @Schema(description = "PPT标题")
    private String title;
    
    @Schema(description = "生成提示词")
    private String prompt;
    
    @Schema(description = "模板ID")
    private String templateId;
    
    @Schema(description = "幻灯片数量")
    private Integer slideCount;
    
    @Schema(description = "PPT文件URL")
    private String pptUrl;
    
    @Schema(description = "PDF文件URL")
    private String pdfUrl;
    
    @Schema(description = "缩略图URL")
    private String thumbnailUrl;
    
    @Schema(description = "生成耗时(秒)")
    private Integer generationTime;
    
    @Schema(description = "状态:0生成中,1成功,2失败")
    private Integer status;
    
    @Schema(description = "状态描述")
    private String statusDesc;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}