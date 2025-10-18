package com.kmu.edu.back_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kmu.edu.back_service.config.SiliconAIConfig;
import com.kmu.edu.back_service.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 硅基流动AI服务客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconAIService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SiliconAIConfig config;
    private final WebClient.Builder webClientBuilder;

    /**
     * 聊天完成（使用默认聊天模型）
     */
    public String chatCompletion(String message, List<Map<String, String>> history) {
        return chatCompletionWithModel(message, history, null);
    }

    /**
     * 聊天完成（可指定模型覆盖）
     */
    public String chatCompletionWithModel(String message, List<Map<String, String>> history, String overrideModel) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // 构建消息列表
            List<Map<String, String>> messages = new java.util.ArrayList<>();

            // 系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个有用的AI助手，请根据用户的问题提供准确、有帮助的回答。");
            messages.add(systemMessage);

            // 历史
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
            }

            // 当前用户消息（非空时）
            if (CommonUtils.isNotEmpty(message)) {
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", message);
                messages.add(userMessage);
            }

            // 请求体
            Map<String, Object> requestBody = new HashMap<>();
            String model = CommonUtils.isNotEmpty(overrideModel) ? overrideModel : config.getChatModel();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("stream", false);

            Mono<Map<String, Object>> responseMono = webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(60));

            Map<String, Object> response = responseMono.block();
            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                    if (messageObj != null) {
                        Object content = messageObj.get("content");
                        if (content instanceof String) {
                            return (String) content;
                        }
                    }
                }
            }
            return "抱歉，我无法理解您的问题，请重新描述一下。";
        } catch (Exception e) {
            log.error("调用硅基流动聊天API失败", e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 聊天流式（SSE/NDJSON）返回增量文本
     */
    public Flux<String> chatCompletionStream(String message, List<Map<String, String>> history, String overrideModel) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // 构建消息列表
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个有用的AI助手，请根据用户的问题提供准确、有帮助的回答。");
            messages.add(systemMessage);
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
            }
            if (CommonUtils.isNotEmpty(message)) {
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", message);
                messages.add(userMessage);
            }

            Map<String, Object> requestBody = new HashMap<>();
            String model = CommonUtils.isNotEmpty(overrideModel) ? overrideModel : config.getChatModel();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("stream", true);

            log.info("发起流式聊天请求: model={}, messages_count={}", model, messages.size());

            return webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(300))
                    .doOnNext(chunk -> log.debug("收到流式数据块: {}", chunk))
                    .flatMap(this::splitAndParseLines)
                    .doOnNext(content -> log.debug("解析出内容: {}", content))
                    .onErrorResume(throwable -> {
                        log.error("流式聊天过程中出现错误: {}", throwable.getMessage(), throwable);
                        // 提供更友好的错误提示
                        if (throwable.getMessage().contains("timeout")) {
                            return Flux.error(new RuntimeException("AI响应超时，请稍后重试"));
                        }
                        return Flux.error(new RuntimeException("AI服务暂时不可用，请稍后重试: " + throwable.getMessage()));
                    });
        } catch (Exception e) {
            log.error("调用硅基流动聊天流式API失败", e);
            return Flux.error(new RuntimeException("AI服务暂时不可用，请稍后重试", e));
        }
    }

    /**
     * 聊天流式（SSE/NDJSON）返回内容与思考片段
     */
    public Flux<StreamPiece> chatCompletionStreamPieces(String message, List<Map<String, String>> history, String overrideModel) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // 构建消息列表
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个有用的AI助手，请根据用户的问题提供准确、有帮助的回答。");
            messages.add(systemMessage);
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
            }
            if (CommonUtils.isNotEmpty(message)) {
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", message);
                messages.add(userMessage);
            }

            Map<String, Object> requestBody = new HashMap<>();
            String model = CommonUtils.isNotEmpty(overrideModel) ? overrideModel : config.getChatModel();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("stream", true);

            log.info("发起流式（含思考）聊天请求: model={}, messages_count={}", model, messages.size());

            return webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(300))
                    .doOnNext(chunk -> log.debug("收到流式数据块: {}", chunk))
                    .flatMap(this::splitAndParseLinesPieces)
                    .doOnNext(piece -> log.debug("解析出片段: type={}, len={}", piece.type, piece.text != null ? piece.text.length() : 0))
                    .onErrorResume(throwable -> {
                        log.error("流式聊天（含思考）过程中出现错误: {}", throwable.getMessage(), throwable);
                        if (throwable.getMessage().contains("timeout")) {
                            return Flux.error(new RuntimeException("AI响应超时，请稍后重试"));
                        }
                        return Flux.error(new RuntimeException("AI服务暂时不可用，请稍后重试: " + throwable.getMessage()));
                    });
        } catch (Exception e) {
            log.error("调用硅基流动聊天流式API失败（含思考）", e);
            return Flux.error(new RuntimeException("AI服务暂时不可用，请稍后重试", e));
        }
    }

    public static class StreamPiece {
        public String type; // "content" 或 "reasoning"
        public String text;
        public StreamPiece() {}
        public StreamPiece(String type, String text) { this.type = type; this.text = text; }
    }

    private Flux<StreamPiece> splitAndParseLinesPieces(String chunk) {
        if (chunk == null || chunk.isEmpty()) return Flux.empty();
        String[] lines = chunk.split("\r?\n");
        StringBuilder dataBuilder = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    continue;
                }
                dataBuilder.append(data);
            } else if (!line.startsWith(":")) {
                dataBuilder.append(line);
            }
        }
        String jsonData = dataBuilder.toString().trim();
        if (jsonData.isEmpty()) return Flux.empty();
        return parseDeltaPieces(jsonData);
    }

    private Flux<StreamPiece> parseDeltaPieces(String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) return Flux.empty();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = MAPPER.readValue(jsonData, Map.class);
            Object choicesObj = obj.get("choices");
            if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
                Object ch0 = ((List<?>) choicesObj).get(0);
                if (ch0 instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String, Object> ch = (Map<String, Object>) ch0;
                    Object deltaObj = ch.get("delta");
                    if (deltaObj instanceof Map) {
                        @SuppressWarnings("unchecked") Map<String, Object> delta = (Map<String, Object>) deltaObj;
                        String c = null, r = null;
                        Object content = delta.get("content");
                        if (content instanceof String s && !s.isEmpty()) c = s;
                        Object reasoning = delta.get("reasoning_content");
                        if (reasoning instanceof String s && !s.isEmpty()) r = s;
                        java.util.List<StreamPiece> list = new java.util.ArrayList<>(2);
                        if (r != null) list.add(new StreamPiece("reasoning", r));
                        if (c != null) list.add(new StreamPiece("content", c));
                        return Flux.fromIterable(list);
                    }
                    // 兼容非增量一次性 message
                    Object msgObj = ch.get("message");
                    if (msgObj instanceof Map) {
                        @SuppressWarnings("unchecked") Map<String, Object> msg = (Map<String, Object>) msgObj;
                        String c = null, r = null;
                        Object content = msg.get("content");
                        if (content instanceof String s && !s.isEmpty()) c = s;
                        Object reasoning = msg.get("reasoning_content");
                        if (reasoning instanceof String s && !s.isEmpty()) r = s;
                        java.util.List<StreamPiece> list = new java.util.ArrayList<>(2);
                        if (r != null) list.add(new StreamPiece("reasoning", r));
                        if (c != null) list.add(new StreamPiece("content", c));
                        return Flux.fromIterable(list);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析流式数据失败: {}, 数据: {}", e.getMessage(), jsonData);
        }
        return Flux.empty();
    }

    private Flux<String> splitAndParseLines(String chunk) {
        if (chunk == null || chunk.isEmpty()) return Flux.empty();
        
        log.debug("处理数据块: {}", chunk);
        
        // 按行分割SSE数据
        String[] lines = chunk.split("\r?\n");
        StringBuilder dataBuilder = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 处理SSE格式
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    log.debug("收到结束标记");
                    continue;
                }
                dataBuilder.append(data);
            } else if (line.startsWith("event: ")) {
                // 事件类型，可以忽略或记录
                log.debug("收到事件类型: {}", line.substring(7));
            } else if (!line.startsWith(":")) {
                // 不是注释行，可能是纯数据
                dataBuilder.append(line);
            }
        }
        
        String jsonData = dataBuilder.toString().trim();
        if (jsonData.isEmpty()) {
            return Flux.empty();
        }
        
        return parseDelta(jsonData);
    }

    private Flux<String> parseDelta(String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) return Flux.empty();
        
        try {
            log.debug("解析JSON数据: {}", jsonData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = MAPPER.readValue(jsonData, Map.class);
            
            Object choicesObj = obj.get("choices");
            if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
                Object ch0 = ((List<?>) choicesObj).get(0);
                if (ch0 instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ch = (Map<String, Object>) ch0;
                    
                    // 检查delta字段
                    Object deltaObj = ch.get("delta");
                    if (deltaObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> delta = (Map<String, Object>) deltaObj;
                        Object content = delta.get("content");
                        if (content instanceof String s && !s.isEmpty()) {
                            log.debug("从delta中提取内容: {}", s);
                            return Flux.just(s);
                        }
                    }
                    
                    // 检查message字段（备用）
                    Object msgObj = ch.get("message");
                    if (msgObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> msg = (Map<String, Object>) msgObj;
                        Object content = msg.get("content");
                        if (content instanceof String s && !s.isEmpty()) {
                            log.debug("从message中提取内容: {}", s);
                            return Flux.just(s);
                        }
                    }
                    
                    // 检查finish_reason
                    Object finishReason = ch.get("finish_reason");
                    if (finishReason != null) {
                        log.debug("完成原因: {}", finishReason);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析流式数据失败: {}, 数据: {}", e.getMessage(), jsonData);
        }
        return Flux.empty();
    }

    /**
     * 图片生成
     */
    public String generateImage(String prompt, String style, String size) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getImageModel());
            requestBody.put("prompt", enhancePromptWithStyle(prompt, style));
            requestBody.put("n", 1);
            requestBody.put("size", size);

            Mono<Map<String, Object>> responseMono = webClient
                    .post()
                    .uri("/images/generations")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(120));

            Map<String, Object> response = responseMono.block();
            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    Map<String, Object> imageData = data.get(0);
                    Object url = imageData.get("url");
                    if (url != null) return url.toString();
                }
            }
            throw new RuntimeException("图片生成失败：API返回空结果");
        } catch (Exception e) {
            log.error("调用硅基流动图片生成API失败", e);
            throw new RuntimeException("图片生成服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 视频生成
     */
    public String generateVideo(String prompt, String imageUrl) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // 1) 提交生成任务
            Map<String, Object> submitBody = new HashMap<>();
            String chosenModel = CommonUtils.isNotEmpty(imageUrl) ? pickI2VModel() : pickT2VModel();
            submitBody.put("model", chosenModel);
            submitBody.put("image_size", config.getVideoImageSize());
            if (CommonUtils.isNotEmpty(imageUrl)) {
                submitBody.put("image", imageUrl);
                if (CommonUtils.isNotEmpty(prompt)) {
                    submitBody.put("prompt", prompt);
                }
            } else {
                submitBody.put("prompt", prompt);
            }

            Mono<Map<String, Object>> submitMono = webClient
                    .post()
                    .uri("/video/submit")
                    .bodyValue(submitBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResp ->
                            clientResp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new RuntimeException("Submit failed: HTTP " + clientResp.statusCode().value() + ", body=" + body)))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(60));

            Map<String, Object> submitResp = submitMono.block();
            if (submitResp == null || !submitResp.containsKey("requestId")) {
                log.error("视频提交失败或无requestId，响应: {}", submitResp);
                throw new RuntimeException("视频生成提交失败");
            }
            String requestId = String.valueOf(submitResp.get("requestId"));

            // 2) 轮询状态直到成功/失败
            long start = System.currentTimeMillis();
            long timeoutMs = Duration.ofSeconds(config.getVideoTimeoutSeconds() != null ? config.getVideoTimeoutSeconds() : 300).toMillis();
            long intervalMs = (long) (config.getVideoPollIntervalMillis() != null ? config.getVideoPollIntervalMillis() : 3000);
            while (true) {
                Map<String, Object> statusBody = new HashMap<>();
                statusBody.put("requestId", requestId);

                Mono<Map<String, Object>> statusMono = webClient
                        .post()
                        .uri("/video/status")
                        .bodyValue(statusBody)
                        .retrieve()
                        .onStatus(status -> !status.is2xxSuccessful(), clientResp ->
                                clientResp.bodyToMono(String.class).defaultIfEmpty("")
                                        .flatMap(body -> Mono.error(new RuntimeException("Status failed: HTTP " + clientResp.statusCode().value() + ", body=" + body)))
                        )
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(60));

                Map<String, Object> statusResp = statusMono.block();
                if (statusResp == null) {
                    log.warn("查询视频状态返回空，requestId={}", requestId);
                } else {
                    String status = String.valueOf(statusResp.get("status"));
                    log.info("视频状态: requestId={}, status={}", requestId, status);
                    if ("Succeed".equalsIgnoreCase(status)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> results = (Map<String, Object>) statusResp.get("results");
                        if (results != null) {
                            Object videosObj = results.get("videos");
                            if (videosObj instanceof List) {
                                List<?> videos = (List<?>) videosObj;
                                if (!videos.isEmpty() && videos.get(0) instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> first = (Map<String, Object>) videos.get(0);
                                    Object url = first.get("url");
                                    if (url != null) {
                                        return url.toString();
                                    }
                                }
                            }
                        }
                        throw new RuntimeException("视频生成成功但无URL");
                    } else if ("Failed".equalsIgnoreCase(status)) {
                        String reason = String.valueOf(statusResp.get("reason"));
                        throw new RuntimeException("视频生成失败：" + reason);
                    }
                }

                if (System.currentTimeMillis() - start > timeoutMs) {
                    throw new RuntimeException("视频生成超时，请稍后在任务列表查看");
                }

                try { Thread.sleep(intervalMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        } catch (Exception e) {
            if (e instanceof WebClientResponseException wce) {
                log.error("调用硅基流动视频生成API失败: status={}, body={}", wce.getStatusCode(), wce.getResponseBodyAsString(), e);
            } else {
                log.error("调用硅基流动视频生成API失败: {}", e.getMessage(), e);
            }
            throw new RuntimeException("视频生成服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 选择文生视频模型（优先 videoT2VModel，其次 videoModel 若包含T2V，否则回退到默认安全值）
     */
    private String pickT2VModel() {
        if (CommonUtils.isNotEmpty(config.getVideoT2VModel())) return config.getVideoT2VModel();
        String vm = config.getVideoModel();
        if (CommonUtils.isNotEmpty(vm) && vm.toUpperCase().contains("T2V")) return vm;
        return "Wan-AI/Wan2.2-T2V-A14B";
    }

    /**
     * 选择图生视频模型（优先 videoI2VModel，其次 videoModel 若包含I2V，否则回退到默认安全值）
     */
    private String pickI2VModel() {
        if (CommonUtils.isNotEmpty(config.getVideoI2VModel())) return config.getVideoI2VModel();
        String vm = config.getVideoModel();
        if (CommonUtils.isNotEmpty(vm) && vm.toUpperCase().contains("I2V")) return vm;
        return "Wan-AI/Wan2.2-I2V-A14B";
    }

    /**
     * 根据风格增强提示词
     */
    private String enhancePromptWithStyle(String prompt, String style) {
        if (CommonUtils.isEmpty(style) || "realistic".equals(style)) {
            return prompt;
        }
        Map<String, String> styleEnhancements = new HashMap<>();
        styleEnhancements.put("cartoon", ", cartoon style, animated, colorful");
        styleEnhancements.put("anime", ", anime style, manga style, japanese animation");
        styleEnhancements.put("oil_painting", ", oil painting style, artistic, classical painting");
        styleEnhancements.put("watercolor", ", watercolor painting style, soft colors, artistic");
        String enhancement = styleEnhancements.getOrDefault(style, "");
        return prompt + enhancement;
    }
}