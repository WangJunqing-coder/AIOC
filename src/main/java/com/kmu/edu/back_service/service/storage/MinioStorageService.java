package com.kmu.edu.back_service.service.storage;

import com.kmu.edu.back_service.config.MinioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service("storageMinioService")
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioProperties props;
    // 通过反射持有 client，避免在依赖未下载前编译失败
    private Object client;

    private synchronized void initClient() {
        if (client != null) return;
        try {
            Class<?> minioClientClz = Class.forName("io.minio.MinioClient");
            Object builder = minioClientClz.getMethod("builder").invoke(null);
            builder = builder.getClass().getMethod("endpoint", String.class).invoke(builder, props.getEndpoint());
            builder = builder.getClass().getMethod("credentials", String.class, String.class)
                    .invoke(builder, props.getAccessKey(), props.getSecretKey());
            client = builder.getClass().getMethod("build").invoke(builder);
        } catch (Throwable e) {
            throw new IllegalStateException("MinIO 客户端初始化失败，请检查依赖与配置", e);
        }
    }

    private void ensureBucket() {
        initClient();
        try {
            Class<?> bucketExistsArgsClz = Class.forName("io.minio.BucketExistsArgs");
            Object bea = bucketExistsArgsClz.getMethod("builder").invoke(null);
            bea = bea.getClass().getMethod("bucket", String.class).invoke(bea, props.getBucket());
            Object beaBuilt = bea.getClass().getMethod("build").invoke(bea);
            boolean exists = (boolean) client.getClass().getMethod("bucketExists", bucketExistsArgsClz).invoke(client, beaBuilt);
            if (!exists) {
                Class<?> makeBucketArgsClz = Class.forName("io.minio.MakeBucketArgs");
                Object mba = makeBucketArgsClz.getMethod("builder").invoke(null);
                mba = mba.getClass().getMethod("bucket", String.class).invoke(mba, props.getBucket());
                Object mbaBuilt = mba.getClass().getMethod("build").invoke(mba);
                client.getClass().getMethod("makeBucket", makeBucketArgsClz).invoke(client, mbaBuilt);
            }
        } catch (Throwable e) {
            log.warn("MinIO 桶检查/创建失败，可能无权限：{}", e.getMessage());
        }
    }

    private String putObject(byte[] data, String objectName, String contentType) {
        initClient();
        ensureBucket();
        try (InputStream is = new ByteArrayInputStream(data)) {
            Class<?> putObjectArgsClz = Class.forName("io.minio.PutObjectArgs");
            Object poa = putObjectArgsClz.getMethod("builder").invoke(null);
            poa = poa.getClass().getMethod("bucket", String.class).invoke(poa, props.getBucket());
            poa = poa.getClass().getMethod("object", String.class).invoke(poa, objectName);
            poa = poa.getClass().getMethod("stream", InputStream.class, long.class, long.class)
                    .invoke(poa, is, (long) data.length, -1L);
            poa = poa.getClass().getMethod("contentType", String.class).invoke(poa, contentType);
            Object poaBuilt = poa.getClass().getMethod("build").invoke(poa);
            client.getClass().getMethod("putObject", putObjectArgsClz).invoke(client, poaBuilt);
        } catch (Throwable e) {
            throw new RuntimeException("上传到 MinIO 失败：" + e.getMessage(), e);
        }
    String base = props.getPublicBaseUrl() != null && !props.getPublicBaseUrl().isEmpty() ? props.getPublicBaseUrl() : props.getEndpoint();
    base = base.endsWith("/") ? base.substring(0, base.length()-1) : base;
    String bucket = props.getBucket();
    String lastSeg = base.contains("/") ? base.substring(base.lastIndexOf('/') + 1) : base;
    boolean baseHasBucket = bucket != null && bucket.equals(lastSeg);
    return base + "/" + (baseHasBucket ? "" : (bucket + "/")) + objectName;
    }

    public String uploadImage(byte[] data, String suffix, String contentType) {
        String day = LocalDate.now().toString();
        String key = "images/" + day + "/" + UUID.randomUUID().toString().replace("-", "") + (suffix != null ? suffix : "");
        return putObject(data, key, contentType != null ? contentType : "image/png");
    }

    public String uploadImageFromUrl(String url) {
        try {
            // 简单下载到内存（若图片过大可改为流式）
            byte[] bytes = cn.hutool.http.HttpUtil.downloadBytes(url);
            String lower = url.toLowerCase();
            String suffix = lower.contains(".png") ? ".png" : lower.contains(".jpg") || lower.contains(".jpeg") ? ".jpg" : lower.contains(".webp") ? ".webp" : ".png";
            String contentType = suffix.equals(".jpg") ? "image/jpeg" : suffix.equals(".webp") ? "image/webp" : "image/png";
            return uploadImage(bytes, suffix, contentType);
        } catch (Exception e) {
            throw new RuntimeException("下载并上传图片失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成对象的预签名GET URL（反射方式，避免编译期依赖）。
     */
    public String presignGet(String bucket, String objectName, int expirySeconds) {
        initClient();
        try {
            Class<?> argsClz = Class.forName("io.minio.GetPresignedObjectUrlArgs");
            Class<?> methodClz = Class.forName("io.minio.http.Method");
            Object getEnum = methodClz.getField("GET").get(null);

            Object builder = argsClz.getMethod("builder").invoke(null);
            builder = builder.getClass().getMethod("method", methodClz).invoke(builder, getEnum);
            builder = builder.getClass().getMethod("bucket", String.class).invoke(builder, bucket);
            builder = builder.getClass().getMethod("object", String.class).invoke(builder, objectName);
            // 兼容不同版本的 expiry 签名（int/Integer）
            try {
                builder = builder.getClass().getMethod("expiry", int.class).invoke(builder, expirySeconds);
            } catch (NoSuchMethodException ex) {
                builder = builder.getClass().getMethod("expiry", Integer.class).invoke(builder, Integer.valueOf(expirySeconds));
            }
            Object built = builder.getClass().getMethod("build").invoke(builder);
            Object url = client.getClass().getMethod("getPresignedObjectUrl", argsClz).invoke(client, built);
            return String.valueOf(url);
        } catch (Throwable e) {
            throw new RuntimeException("生成预签名URL失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将以 public-url/endpoint 开头的公开URL转换为预签名GET URL。
     */
    public String generatePresignedGetUrlForPublicUrl(String publicUrl, int expirySeconds) {
        String base = props.getPublicBaseUrl() != null && !props.getPublicBaseUrl().isEmpty() ? props.getPublicBaseUrl() : props.getEndpoint();
        if (publicUrl == null || base == null || !publicUrl.startsWith(base)) {
            throw new IllegalArgumentException("URL 不在允许的 MinIO 域名下");
        }
        String bucket = props.getBucket();
        String trimmedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String lastSeg = trimmedBase.contains("/") ? trimmedBase.substring(trimmedBase.lastIndexOf('/') + 1) : trimmedBase;
        boolean baseHasBucket = bucket != null && bucket.equals(lastSeg);

        String tail = publicUrl.substring(trimmedBase.length());
        if (tail.startsWith("/")) tail = tail.substring(1);

        String objectName = tail;
        // 兼容历史保存的 URL 中出现的双重编码，如 %252F -> %2F -> '/'
        for (int i = 0; i < 3; i++) {
            try {
                String decoded = java.net.URLDecoder.decode(objectName, java.nio.charset.StandardCharsets.UTF_8);
                if (decoded.equals(objectName)) break;
                objectName = decoded;
            } catch (Exception ignore) { break; }
        }
        if (baseHasBucket) {
            // base 已包含桶名，tail 即 objectName
            // no-op
        } else if (objectName.startsWith(bucket + "/")) {
            // base 不含桶名，但 URL 中紧随其后为已配置的桶名
            objectName = objectName.substring(bucket.length() + 1);
        } else {
            // 兜底：尝试从 URL 中解析桶名
            int idx = objectName.indexOf('/');
            if (idx > 0) {
                String bucketFromUrl = objectName.substring(0, idx);
                String objFromUrl = objectName.substring(idx + 1);
                if (bucketFromUrl != null && !bucketFromUrl.isEmpty()) {
                    bucket = bucketFromUrl;
                    objectName = objFromUrl;
                }
            }
        }
        return presignGet(bucket, objectName, expirySeconds);
    }
}
