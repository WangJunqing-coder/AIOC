package com.kmu.edu.back_service.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频生成记录实体
 */
@Data
public class VideoGeneration {
    private Long id;
    private Long userId;
    private String prompt;
    private Integer sourceType;      // 1 文本 2 图片
    private String sourceImageUrl;
    private Integer duration;        // 秒
    private String style;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer generationTime;  // 秒
    private Integer progress;        // 0-100
    private Integer status;          // 0生成中 1成功 2失败
    private String errorMessage;
    private LocalDateTime createTime;
}
