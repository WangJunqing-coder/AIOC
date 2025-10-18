package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PPT生成请求
 */
@Data
@Schema(description = "PPT生成请求")
public class PptGenerationRequest {
    
    @NotBlank(message = "PPT标题不能为空")
    @Size(max = 200, message = "PPT标题长度不能超过200个字符")
    @Schema(description = "PPT标题", example = "人工智能发展趋势")
    private String title;
    
    @NotBlank(message = "生成提示词不能为空")
    @Size(max = 2000, message = "提示词长度不能超过2000个字符")
    @Schema(description = "生成提示词", example = "请生成一个关于人工智能发展趋势的PPT，包含AI技术发展历程、当前应用场景、未来发展方向等内容")
    private String prompt;
    
    @Schema(description = "模板ID", example = "business_template_01")
    private String templateId;
    
    @Schema(description = "期望的幻灯片数量", example = "10")
    private Integer slideCount;
    
    @Schema(description = "PPT风格", example = "商务")
    private String style;
}