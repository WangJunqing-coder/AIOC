package com.kmu.edu.back_service.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserUsageRecord {
    private Long id;
    private Long userId;
    private Integer serviceType;     // 1聊天 2图片 3视频 4PPT
    private Integer tokensUsed;
    private java.math.BigDecimal cost;
    private String requestContent;
    private String responseContent;
    private Integer status;          // 0失败 1成功
    private LocalDateTime createTime;
}
