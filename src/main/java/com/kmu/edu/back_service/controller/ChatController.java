package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.request.ChatRequest;
import com.kmu.edu.back_service.dto.request.ImageChatMessageRequest;
import com.kmu.edu.back_service.dto.response.ChatResponse;
import com.kmu.edu.back_service.entity.ChatMessage;
import com.kmu.edu.back_service.entity.ChatSession;
import com.kmu.edu.back_service.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Validated
@Tag(name = "聊天")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    @Operation(summary = "发送聊天消息（非流式）")
    @SaCheckLogin
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送聊天消息（SSE流式）")
    @SaCheckLogin
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        return chatService.chatStream(request);
    }

    @PostMapping("/session")
    @Operation(summary = "创建会话")
    @SaCheckLogin
    public Result<Map<String, String>> createSession(@RequestParam(value = "title", required = false) String title) {
        Long userId = StpUtil.getLoginIdAsLong();
        String sid = chatService.createSession(userId, title);
        Map<String, String> data = new HashMap<>();
        data.put("sessionId", sid);
        return Result.success(data);
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取用户会话列表")
    @SaCheckLogin
    public Result<List<ChatSession>> sessions(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                              @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(chatService.getUserSessions(userId, page, size));
    }

    @GetMapping("/session/{sessionId}/messages")
    @Operation(summary = "获取会话消息列表")
    @SaCheckLogin
    public Result<List<ChatMessage>> messages(@PathVariable("sessionId") String sessionId,
                                              @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                              @RequestParam(value = "size", required = false, defaultValue = "50") Integer size) {
        return Result.success(chatService.getSessionMessages(sessionId, page, size));
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话")
    @SaCheckLogin
    public Result<Void> delete(@PathVariable("sessionId") String sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success();
    }

    @PostMapping("/session/{sessionId}/messages/image")
    @Operation(summary = "追加图片消息到会话")
    @SaCheckLogin
    public Result<Void> appendImage(@PathVariable("sessionId") String sessionId,
                                    @Valid @RequestBody ImageChatMessageRequest request) {
        chatService.appendImageMessage(sessionId, request);
        return Result.success();
    }
}
