package com.kmu.edu.back_service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kmu.edu.back_service.common.ResultCode;
import com.kmu.edu.back_service.constant.Constants;
import com.kmu.edu.back_service.dto.request.ChatRequest;
import com.kmu.edu.back_service.dto.request.ImageChatMessageRequest;
import com.kmu.edu.back_service.dto.response.ChatResponse;
import com.kmu.edu.back_service.entity.ChatMessage;
import com.kmu.edu.back_service.entity.ChatSession;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.mapper.ChatMessageMapper;
import com.kmu.edu.back_service.mapper.ChatSessionMapper;
import com.kmu.edu.back_service.service.ChatService;
import com.kmu.edu.back_service.service.SysUserService;
import com.kmu.edu.back_service.utils.CommonUtils;
import com.kmu.edu.back_service.service.SiliconAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.time.LocalDateTime;
import java.util.List;
 

/**
 * 聊天服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final SysUserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SiliconAIService siliconAIService;
    private final com.kmu.edu.back_service.mapper.UserUsageRecordMapper userUsageRecordMapper;
    
    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        long startTime = System.currentTimeMillis();
        
        // 检查每日使用限制
        userService.checkDailyLimit(userId, Constants.ServiceType.CHAT);
        
        // 检查并扣减tokens
        if (!userService.useTokens(userId, Constants.TokenCost.CHAT)) {
            throw new BusinessException(ResultCode.INSUFFICIENT_TOKENS);
        }
        
        try {
            // 获取或创建会话
            ChatSession session = getOrCreateSession(request, userId);
            
            // 保存用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSessionId(session.getId());
            userMessage.setUserId(userId);
            userMessage.setMessageType(Constants.MessageType.USER);
            userMessage.setContent(request.getMessage());
            userMessage.setTokenCount(Constants.TokenCost.CHAT);
            messageMapper.insert(userMessage);
            
            // 构建AI对话上下文（消息历史）
            List<com.kmu.edu.back_service.entity.ChatMessage> recent = request.getKeepContext() ?
                    messageMapper.selectRecentBySessionId(session.getId(), 10) : java.util.Collections.emptyList();
            List<java.util.Map<String, String>> history = new java.util.ArrayList<>();
            if (recent != null) {
                for (int i = recent.size() - 1; i >= 0; i--) {
                    var msg = recent.get(i);
                    String role = msg.getMessageType() == Constants.MessageType.USER ? "user" : "assistant";
                    java.util.Map<String, String> item = new java.util.HashMap<>();
                    item.put("role", role);
                    item.put("content", msg.getContent());
                    history.add(item);
                }
            }

            // 调用AI服务（支持深度思考模型切换）
            String overrideModel = Boolean.TRUE.equals(request.getDeepThink()) ? "deepseek-ai/DeepSeek-R1" : null;
            String aiReply = siliconAIService.chatCompletionWithModel(request.getMessage(), history, overrideModel);
            
            // 保存AI回复
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSessionId(session.getId());
            aiMessage.setUserId(userId);
            aiMessage.setMessageType(Constants.MessageType.AI);
            aiMessage.setContent(aiReply);
            aiMessage.setTokenCount(0); // AI回复不消耗用户token
            messageMapper.insert(aiMessage);
            
            // 更新会话信息
            sessionMapper.incrementMessageCount(session.getSessionId());
            
            // 如果是新会话，生成标题
            if (session.getMessageCount() == 0) {
                String title = generateSessionTitle(request.getMessage());
                session.setTitle(title);
                sessionMapper.update(session);
            }
            
            // 构建响应
            ChatResponse response = new ChatResponse();
            response.setSessionId(session.getSessionId());
            response.setReply(aiReply);
            response.setTokenUsed(Constants.TokenCost.CHAT);
            response.setResponseTime(System.currentTimeMillis() - startTime);
            response.setCreateTime(LocalDateTime.now());
            
            log.info("聊天完成：用户ID={}, 会话ID={}, 耗时={}ms", userId, session.getSessionId(), response.getResponseTime());
            // 写入使用记录
            com.kmu.edu.back_service.entity.UserUsageRecord record = new com.kmu.edu.back_service.entity.UserUsageRecord();
            record.setUserId(userId);
            record.setServiceType(Constants.ServiceType.CHAT);
            record.setTokensUsed(Constants.TokenCost.CHAT);
            record.setRequestContent(request.getMessage());
            record.setResponseContent(aiReply);
            record.setStatus(1);
            userUsageRecordMapper.insert(record);
            return response;
            
        } catch (Exception e) {
            // 如果出错，退还tokens
            userService.addTokens(userId, Constants.TokenCost.CHAT);
            // 写入失败记录
            com.kmu.edu.back_service.entity.UserUsageRecord record = new com.kmu.edu.back_service.entity.UserUsageRecord();
            record.setUserId(userId);
            record.setServiceType(Constants.ServiceType.CHAT);
            record.setTokensUsed(0);
            record.setRequestContent(request.getMessage());
            record.setResponseContent(e.getMessage());
            record.setStatus(0);
            try { userUsageRecordMapper.insert(record); } catch (Exception ignore) {}
            log.error("聊天失败：用户ID={}, 错误信息={}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务暂时不可用，请稍后重试");
        }
    }

    @Override
    @Transactional
    public SseEmitter chatStream(ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        long startTime = System.currentTimeMillis();

        // 检查每日使用限制
        userService.checkDailyLimit(userId, Constants.ServiceType.CHAT);

        // 检查并扣减tokens
        if (!userService.useTokens(userId, Constants.TokenCost.CHAT)) {
            throw new BusinessException(ResultCode.INSUFFICIENT_TOKENS);
        }

        // SseEmitter，超时时间设置长一些，适应长文本生成
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        try {
            ChatSession session = getOrCreateSession(request, userId);

            // 保存用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSessionId(session.getId());
            userMessage.setUserId(userId);
            userMessage.setMessageType(Constants.MessageType.USER);
            userMessage.setContent(request.getMessage());
            userMessage.setTokenCount(Constants.TokenCost.CHAT);
            messageMapper.insert(userMessage);

            // 历史上下文
            List<com.kmu.edu.back_service.entity.ChatMessage> recent = Boolean.TRUE.equals(request.getKeepContext()) ?
                    messageMapper.selectRecentBySessionId(session.getId(), 10) : java.util.Collections.emptyList();
            java.util.List<java.util.Map<String, String>> history = new java.util.ArrayList<>();
            if (recent != null) {
                for (int i = recent.size() - 1; i >= 0; i--) {
                    var msg = recent.get(i);
                    String role = msg.getMessageType() == Constants.MessageType.USER ? "user" : "assistant";
                    java.util.Map<String, String> item = new java.util.HashMap<>();
                    item.put("role", role);
                    item.put("content", msg.getContent());
                    history.add(item);
                }
            }

            String overrideModel = Boolean.TRUE.equals(request.getDeepThink()) ? "deepseek-ai/DeepSeek-R1" : null;

            final StringBuilder total = new StringBuilder();
            final StringBuilder reasoningBuf = new StringBuilder();

            // 发送开始事件
            try {
                emitter.send(SseEmitter.event().name("start").data("开始生成回答..."));
            } catch (Exception e) {
                log.warn("发送开始事件失败: {}", e.getMessage());
            }

            // 订阅增量并转发给前端（包含内容与思考片段）
            Disposable disposable = siliconAIService.chatCompletionStreamPieces(request.getMessage(), history, overrideModel)
                    .doOnNext(piece -> {
                        try {
                            if (piece == null || piece.text == null || piece.text.isEmpty()) return;
                            if ("reasoning".equals(piece.type)) {
                                reasoningBuf.append(piece.text);
                                emitter.send(SseEmitter.event().name("reasoning").data(piece.text));
                            } else {
                                total.append(piece.text);
                                emitter.send(SseEmitter.event().name("delta").data(piece.text).reconnectTime(0));
                            }
                        } catch (Exception e) {
                            log.warn("SSE 发送失败: {}", e.getMessage());
                        }
                    })
                    .doOnError(err -> {
                        try {
                            userService.addTokens(userId, Constants.TokenCost.CHAT);
                            emitter.send(SseEmitter.event().name("error").data("AI服务错误：" + err.getMessage()));
                        } catch (Exception ignored) {}
                        try { 
                            emitter.completeWithError(err); 
                        } catch (Exception ignored) {}
                        log.error("流式聊天失败: {}", err.getMessage(), err);
                    })
                    .doOnComplete(() -> {
                        try {
                            String aiReply = total.toString();
                            if (aiReply.isEmpty()) {
                                aiReply = "抱歉，我暂时无法生成回答，请重试。";
                            }

                            // 保存AI消息
                            ChatMessage aiMessage = new ChatMessage();
                            aiMessage.setSessionId(session.getId());
                            aiMessage.setUserId(userId);
                            aiMessage.setMessageType(Constants.MessageType.AI);
                            // 若存在推理过程，将内容与推理一并以JSON存储，避免改表
                            if (reasoningBuf.length() > 0) {
                                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                                payload.put("type", "chat");
                                payload.put("content", aiReply);
                                payload.put("reasoning", reasoningBuf.toString());
                                aiMessage.setContent(com.kmu.edu.back_service.utils.CommonUtils.toJsonString(payload));
                            } else {
                                aiMessage.setContent(aiReply);
                            }
                            aiMessage.setTokenCount(0);
                            messageMapper.insert(aiMessage);

                            // 更新会话信息
                            sessionMapper.incrementMessageCount(session.getSessionId());

                            // 新会话生成标题
                            if (session.getMessageCount() == 0) {
                                String title = generateSessionTitle(request.getMessage());
                                session.setTitle(title);
                                sessionMapper.update(session);
                            }

                            // 使用记录
                            com.kmu.edu.back_service.entity.UserUsageRecord record = new com.kmu.edu.back_service.entity.UserUsageRecord();
                            record.setUserId(userId);
                            record.setServiceType(Constants.ServiceType.CHAT);
                            record.setTokensUsed(Constants.TokenCost.CHAT);
                            record.setRequestContent(request.getMessage());
                            record.setResponseContent(aiReply);
                            record.setStatus(1);
                            try { userUsageRecordMapper.insert(record); } catch (Exception ignored) {}

                            long cost = System.currentTimeMillis() - startTime;
                            log.info("流式聊天完成：用户ID={}, 会话ID={}, 耗时={}ms, 字符数={}", 
                                    userId, session.getSessionId(), cost, aiReply.length());

                            // 发送完成事件
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("{\"sessionId\":\"" + session.getSessionId() + "\",\"tokenUsed\":" + Constants.TokenCost.CHAT + ",\"responseTime\":" + cost + "}"));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("完成流式聊天时出错: {}", e.getMessage(), e);
                            try { 
                                emitter.send(SseEmitter.event().name("error").data("保存回答时出错"));
                                emitter.completeWithError(e); 
                            } catch (Exception ignored) {}
                        }
                    })
                    .subscribe();

            // 连接关闭时，释放订阅
            emitter.onCompletion(() -> {
                try { 
                    if (!disposable.isDisposed()) {
                        disposable.dispose(); 
                    }
                } catch (Exception ignored) {}
                log.debug("SSE连接已关闭：用户ID={}", userId);
            });
            
            emitter.onTimeout(() -> {
                try { 
                    if (!disposable.isDisposed()) {
                        disposable.dispose(); 
                    }
                } catch (Exception ignored) {}
                try { 
                    emitter.send(SseEmitter.event().name("timeout").data("响应超时"));
                    emitter.complete(); 
                } catch (Exception ignored) {}
                log.warn("SSE连接超时：用户ID={}", userId);
            });

            emitter.onError((throwable) -> {
                try { 
                    if (!disposable.isDisposed()) {
                        disposable.dispose(); 
                    }
                } catch (Exception ignored) {}
                log.error("SSE连接出错：用户ID={}, 错误: {}", userId, throwable.getMessage());
            });

            return emitter;
        } catch (Exception e) {
            // 失败退还 token
            try { userService.addTokens(userId, Constants.TokenCost.CHAT); } catch (Exception ignored) {}
            try { 
                emitter.send(SseEmitter.event().name("error").data("服务异常: " + e.getMessage())); 
                emitter.completeWithError(e); 
            } catch (Exception ignored) {}
            log.error("创建流式聊天失败：用户ID={}, 错误: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public String createSession(Long userId, String title) {
        String sessionId = CommonUtils.generateSessionId();
        
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setTitle(CommonUtils.isNotEmpty(title) ? title : "新对话");
        session.setMessageCount(0);
        session.setStatus(1);
        
        sessionMapper.insert(session);
        
        log.info("创建新会话：用户ID={}, 会话ID={}", userId, sessionId);
        return sessionId;
    }
    
    @Override
    public List<ChatSession> getUserSessions(Long userId, Integer page, Integer size) {
        int offset = CommonUtils.calculateOffset(page, size);
        return sessionMapper.selectByUserId(userId, offset, size);
    }
    
    @Override
    public List<ChatMessage> getSessionMessages(String sessionId, Integer page, Integer size) {
        // 验证会话所有权
        ChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.CHAT_SESSION_NOT_FOUND);
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        int offset = CommonUtils.calculateOffset(page, size);
        return messageMapper.selectBySessionId(session.getId(), offset, size);
    }
    
    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        // 验证会话所有权
        ChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.CHAT_SESSION_NOT_FOUND);
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        // 软删除会话
        sessionMapper.deleteBySessionId(sessionId);
        
        // 清除缓存
        String cacheKey = Constants.CacheKey.CHAT_SESSION + sessionId;
        redisTemplate.delete(cacheKey);
        
        log.info("删除会话：用户ID={}, 会话ID={}", userId, sessionId);
    }
    
    @Override
    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        // 验证会话所有权
        ChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.CHAT_SESSION_NOT_FOUND);
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        
        session.setTitle(title);
        sessionMapper.update(session);
        
        log.info("更新会话标题：用户ID={}, 会话ID={}, 标题={}", userId, sessionId, title);
    }
    
    @Override
    public String generateSessionTitle(String firstMessage) {
        try {
            if (!CommonUtils.isNotEmpty(firstMessage)) {
                return "新对话";
            }
            // 基于首条消息生成：去除多余空白/换行/标记，截断到 20 字符
            String title = firstMessage
                    .replaceAll("[\r\n\t]+", " ")          // 统一空白
                    .replaceAll("\\s+", " ")                 // 折叠空格
                    .trim();
            // 去除常见前缀符号（列表/标题符号等）
            title = title.replaceAll("^([#>*\\u2022\\-\\d\\.\\)\\s]+)", "");
            // 基本清理（中英文引号/书名号）
            title = title.replaceAll("[\"'《》]", "");
            if (title.length() > 20) {
                title = title.substring(0, 20);
            }
            return CommonUtils.isNotEmpty(title) ? title : "新对话";
        } catch (Exception e) {
            log.warn("生成会话标题失败，使用默认标题", e);
            return "新对话";
        }
    }
    
    /**
     * 获取或创建会话
     */
    private ChatSession getOrCreateSession(ChatRequest request, Long userId) {
        if (CommonUtils.isNotEmpty(request.getSessionId())) {
            // 使用现有会话
            ChatSession session = sessionMapper.selectBySessionId(request.getSessionId());
            if (session != null && session.getUserId().equals(userId)) {
                return session;
            }
        }
        
        // 创建新会话
        String sessionId = createSession(userId, null);
        return sessionMapper.selectBySessionId(sessionId);
    }
    
    // 删除原基于字符串 context 的封装方法，直接使用 siliconAIService.chatCompletionWithModel

    @Override
    @Transactional
    public void appendImageMessage(String sessionId, ImageChatMessageRequest request) {
        // 验证会话所有权
        ChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.CHAT_SESSION_NOT_FOUND);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 若是新会话，基于提示词生成标题
        if (session.getMessageCount() == 0) {
            try {
                String title = generateSessionTitle(request.getPrompt());
                session.setTitle(title);
                sessionMapper.update(session);
            } catch (Exception ignored) {}
        }

        // 保存用户提示词消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setUserId(userId);
        userMessage.setMessageType(Constants.MessageType.USER);
        userMessage.setContent(request.getPrompt());
        userMessage.setTokenCount(0);
        messageMapper.insert(userMessage);

        // 保存AI图片消息（将图片URL列表序列化为JSON）
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "image");
        payload.put("imageUrls", request.getImageUrls());
        String contentJson = com.kmu.edu.back_service.utils.CommonUtils.toJsonString(payload);

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(session.getId());
        aiMessage.setUserId(userId);
        aiMessage.setMessageType(Constants.MessageType.AI);
        aiMessage.setContent(contentJson);
        aiMessage.setTokenCount(0);
        messageMapper.insert(aiMessage);

        // 更新会话消息计数
        sessionMapper.incrementMessageCount(sessionId);

        log.info("会话追加图片消息：sessionId={}, urls={}", sessionId, request.getImageUrls());
    }
}