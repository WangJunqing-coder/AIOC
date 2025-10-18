package com.kmu.edu.back_service.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
public class ChatMessage {
    
    /**
     * 消息ID
     */
    private Long id;
    
    /**
     * 会话ID
     */
    private Long sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 消息类型:1用户消息,2AI回复
     */
    private Integer messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * token数量
     */
    private Integer tokenCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}