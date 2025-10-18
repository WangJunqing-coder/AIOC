package com.kmu.edu.back_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    // 对外访问基础URL（可选），未配置则使用 endpoint；可包含桶名（path-style）
    private String publicBaseUrl;
}
