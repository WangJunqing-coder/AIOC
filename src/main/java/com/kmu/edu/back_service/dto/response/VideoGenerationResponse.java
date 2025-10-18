package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "视频生成响应")
public class VideoGenerationResponse {
    @Schema(description = "生成ID")
    private Long id;
    @Schema(description = "状态:0生成中,1成功,2失败")
    private Integer status;
    @Schema(description = "进度(0-100)")
    private Integer progress;
    @Schema(description = "视频URL")
    private String videoUrl;
    @Schema(description = "缩略图URL")
    private String thumbnailUrl;
    @Schema(description = "生成耗时(秒)")
    private Integer generationTime;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
