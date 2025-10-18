package com.kmu.edu.back_service.service;

import com.kmu.edu.back_service.dto.request.ChatRequest;
import com.kmu.edu.back_service.dto.request.ImageChatMessageRequest;
import com.kmu.edu.back_service.dto.response.ChatResponse;
import com.kmu.edu.back_service.entity.ChatSession;
import com.kmu.edu.back_service.entity.ChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {
    
    /**
     * 发送聊天消息
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天（SSE）
     */
    SseEmitter chatStream(ChatRequest request);
    
    /**
     * 创建新会话
     */
    String createSession(Long userId, String title);
    
    /**
     * 获取用户会话列表
     */
    List<ChatSession> getUserSessions(Long userId, Integer page, Integer size);
    
    /**
     * 获取会话消息列表
     */
    List<ChatMessage> getSessionMessages(String sessionId, Integer page, Integer size);
    
    /**
     * 删除会话
     */
    void deleteSession(String sessionId);
    
    /**
     * 更新会话标题
     */
    void updateSessionTitle(String sessionId, String title);
    
    /**
     * 生成会话标题
     */
    String generateSessionTitle(String firstMessage);

    /**
     * 追加图片消息到会话
     */
    void appendImageMessage(String sessionId, ImageChatMessageRequest request);
}