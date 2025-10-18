package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片生成响应DTO
 */
@Data
@Schema(description = "图片生成响应")
public class ImageGenerationResponse {
    
    @Schema(description = "生成ID")
    private Long id;
    
    @Schema(description = "生成状态:0生成中,1成功,2失败")
    private Integer status;
    
    @Schema(description = "图片URL列表")
    private List<String> imageUrls;
    
    @Schema(description = "缩略图URL列表")
    private List<String> thumbnailUrls;
    
    @Schema(description = "生成耗时(秒)")
    private Integer generationTime;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}