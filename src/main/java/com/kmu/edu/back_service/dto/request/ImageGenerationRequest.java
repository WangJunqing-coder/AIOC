package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;



/**
 * 图片生成请求DTO
 */
@Data
@Schema(description = "图片生成请求")
public class ImageGenerationRequest {
    
    @NotBlank(message = "生成提示词不能为空")
    @Size(max = 1000, message = "提示词不能超过1000个字符")
    @Schema(description = "图片生成提示词", example = "一只可爱的小猫咪在花园里玩耍")
    private String prompt;
    
    @Schema(description = "图片风格", example = "realistic", allowableValues = {"realistic", "cartoon", "anime", "oil_painting", "watercolor"})
    private String style = "realistic";
    
    @Schema(description = "图片尺寸", example = "1024x1024", allowableValues = {"512x512", "1024x1024", "1024x1792", "1792x1024"})
    private String size = "1024x1024";
    
    @Schema(description = "生成数量", example = "1")
    private Integer count = 1;
    
    @Schema(description = "高清修复", example = "false")
    private Boolean hd = false;
}