package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 视频生成请求DTO
 */
@Data
@Schema(description = "视频生成请求")
public class VideoGenerationRequest {
    
    @Schema(description = "生成提示词（文本生成视频时必填）", example = "一只小猫在花园里玩耍的视频")
    @Size(max = 1000, message = "提示词不能超过1000个字符")
    private String prompt;
    
    @Schema(description = "源图片URL（图片转视频时必填）", example = "http://example.com/image.jpg")
    private String sourceImageUrl;
    
    @Schema(description = "视频时长（秒）", example = "5")
    private Integer duration = 5;
    
    @Schema(description = "视频风格", example = "realistic")
    private String style = "realistic";
    
    @Schema(description = "来源类型", example = "1", allowableValues = {"1", "2"})
    private Integer sourceType = 1; // 1:文本生成, 2:图片转视频
}