package com.kmu.edu.back_service.service;

import com.kmu.edu.back_service.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
            }
        } catch (Exception e) {
            log.error("确保MinIO桶存在失败", e);
            throw new RuntimeException("对象存储不可用", e);
        }
    }

    public String upload(String objectName, InputStream stream, long size, String contentType) {
        ensureBucket();
        try {
            // 当 size 不可知时，使用 10MiB 的分片大小（MinIO 要求 partSize >= 5MiB）
            long partSize = 10L * 1024 * 1024;
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .contentType(contentType);
            if (size >= 0) {
                builder.stream(stream, size, partSize);
            } else {
                builder.stream(stream, -1, partSize);
            }
            minioClient.putObject(builder.build());
            return buildPublicUrl(objectName);
        } catch (Exception e) {
            log.error("上传到MinIO失败: {}", objectName, e);
            throw new RuntimeException("上传失败", e);
        }
    }

    public String buildPublicUrl(String objectName) {
        String base = minioConfig.getPublicBaseUrl();
        if (base != null && !base.isEmpty()) {
            String trimmed = base.replaceAll("/+$", "");
            String bucket = minioConfig.getBucket();
            String lastSeg = trimmed.contains("/") ? trimmed.substring(trimmed.lastIndexOf('/') + 1) : trimmed;
            boolean endsWithBucket = bucket != null && bucket.equals(lastSeg);
            // 不对 objectName 做整体 URL 编码，避免将 '/' 变为 %2F 导致 404
            String safePath = objectName; // 如果需要对空格等做编码，可只替换空格为 %20
            return endsWithBucket ? (trimmed + "/" + safePath) : (trimmed + "/" + bucket + "/" + safePath);
        }
        // 回退为直连MinIO路径（需要MinIO允许公共读或签名访问）
        return minioConfig.getEndpoint().replaceAll("/+$", "") + "/" + minioConfig.getBucket() + "/" + objectName;
    }
}
