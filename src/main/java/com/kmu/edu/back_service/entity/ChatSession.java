package com.kmu.edu.back_service.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体
 */
@Data
public class ChatSession {
    
    /**
     * 会话ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会话唯一标识
     */
    private String sessionId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 上下文摘要
     */
    private String contextSummary;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 状态:0已删除,1正常
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}