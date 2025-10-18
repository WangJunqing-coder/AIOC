package com.kmu.edu.back_service.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage.minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    /** 对外访问的基础URL，如果网关或Nginx做了映射，则填该前缀，用于拼接返回给前端的URL */
    private String publicBaseUrl;
    /** 是否启用Path-Style访问，某些兼容S3的服务需要 */
    private Boolean pathStyleAccess = Boolean.TRUE;

    @Bean
    public MinioClient minioClient() {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey);
        // MinIO新版SDK默认支持，若需额外配置可在此扩展
        return builder.build();
    }
}
