package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天响应DTO
 */
@Data
@Schema(description = "聊天响应")
public class ChatResponse {
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "AI回复内容")
    private String reply;
    
    @Schema(description = "消耗的token数量")
    private Integer tokenUsed;
    
    @Schema(description = "响应时间(毫秒)")
    private Long responseTime;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

/**
 * 聊天会话响应DTO
 */
@Data
@Schema(description = "聊天会话")
class ChatSessionResponse {
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "会话标题")
    private String title;
    
    @Schema(description = "消息数量")
    private Integer messageCount;
    
    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageTime;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

/**
 * 聊天消息响应DTO
 */
@Data
@Schema(description = "聊天消息")
class ChatMessageResponse {
    
    @Schema(description = "消息ID")
    private Long id;
    
    @Schema(description = "消息类型:1用户消息,2AI回复")
    private Integer messageType;
    
    @Schema(description = "消息内容")
    private String content;
    
    @Schema(description = "token数量")
    private Integer tokenCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}