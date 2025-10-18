package com.kmu.edu.back_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 硅基流动AI配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.silicon")
public class SiliconAIConfig {
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 基础URL
     */
    private String baseUrl = "https://api.siliconflow.cn/v1";
    
    /**
     * 聊天模型
     */
    private String chatModel = "Qwen/Qwen3-30B-A3B-Instruct-2507";
    
    /**
     * 图片生成模型
     */
    private String imageModel = "Qwen/Qwen-Image";
    
    /**
     * （兼容字段）视频生成模型，若未配置 videoT2VModel / videoI2VModel，则回退使用该值
     */
    private String videoModel = "Wan-AI/Wan2.2-T2V-A14B";
    /**
     * 文生视频模型（Text-to-Video）
     */
    private String videoT2VModel = "Wan-AI/Wan2.2-T2V-A14B";
    /**
     * 图生视频模型（Image-to-Video）
     */
    private String videoI2VModel = "Wan-AI/Wan2.2-I2V-A14B";
    /**
     * 文/图生视频的默认分辨率（image_size），可选：1280x720、720x1280、960x960
     */
    private String videoImageSize = "1280x720";
    /**
     * 视频生成轮询超时（秒）
     */
    private Integer videoTimeoutSeconds = 300; // 5分钟
    /**
     * 视频生成轮询间隔（毫秒）
     */
    private Integer videoPollIntervalMillis = 3000; // 3秒
    
    /**
     * 超时时间
     */
    private String timeout = "60s";
    
    /**
     * 最大token数
     */
    private Integer maxTokens = 2048;
    
    /**
     * 温度参数
     */
    private Double temperature = 0.7;
}