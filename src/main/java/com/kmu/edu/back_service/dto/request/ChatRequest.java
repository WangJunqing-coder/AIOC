package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;



/**
 * 聊天请求DTO
 */
@Data
@Schema(description = "聊天请求")
public class ChatRequest {
    
    @Schema(description = "会话ID，不传则创建新会话")
    private String sessionId;
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "用户消息内容", example = "你好，请介绍一下人工智能")
    private String message;
    
    @Schema(description = "是否保持上下文", example = "true")
    private Boolean keepContext = true;

    @Schema(description = "是否开启深度思考（将切换到 deepseek-ai/DeepSeek-R1 模型）", example = "false")
    private Boolean deepThink = false;
    
    @Schema(description = "模型温度参数(0-1)", example = "0.7")
    private Double temperature = 0.7;
    
    @Schema(description = "最大token数", example = "2048")
    private Integer maxTokens = 2048;
}