package com.kmu.edu.back_service.controller;

import com.kmu.edu.back_service.common.ResultCode;
import com.kmu.edu.back_service.config.MinioConfig;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.service.MinioStorageService;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;

@RestController
@RequestMapping("/api")
public class ProxyController {

    private final MinioConfig minioConfig;
    private final MinioStorageService storageService;

    public ProxyController(MinioConfig minioConfig, MinioStorageService storageService) {
        this.minioConfig = minioConfig;
        this.storageService = storageService;
    }

    /**
     * 简单同源代理：仅允许代理到配置的 MinIO public-base-url 对应的主机，防止 SSRF。
     * 支持 Range 头部以便视频按需播放。
     */
    @GetMapping("/proxy")
    public void proxy(@RequestParam("url") String url,
                      HttpServletRequest request,
                      HttpServletResponse response) throws Exception {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "url 参数不能为空");
        }
        URI target = URI.create(url);
        validateTarget(target);

        String objectName = resolveObjectName(target.getPath());
        if (!StringUtils.hasText(objectName)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无法解析对象路径");
        }
        if (objectName.contains("..")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非法对象路径");
        }

        StatObjectResponse stat = storageService.statObject(objectName);
        long totalSize = stat.size();

        RangeSpec rangeSpec = parseRange(request.getHeader(HttpHeaders.RANGE), totalSize);
        long offset = rangeSpec.offset();
        Long length = rangeSpec.length();

        response.setHeader("Accept-Ranges", "bytes");
        String contentType = stat.contentType();
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        response.setContentType(contentType);

        if (rangeSpec.partial()) {
            response.setStatus(SC_PARTIAL_CONTENT);
            long end = rangeSpec.end();
            response.setHeader(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", offset, end, totalSize));
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeSpec.length()));
        } else {
            response.setStatus(SC_OK);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(totalSize));
        }

        try (GetObjectResponse objectStream = storageService.getObject(objectName, offset, length)) {
            StreamUtils.copy(objectStream, response.getOutputStream());
        } catch (IOException e) {
            throw e;
        }
    }

    private void validateTarget(URI target) {
        String scheme = target.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅允许代理 http/https 资源");
        }
        String allowBase = minioConfig.getPublicBaseUrl();
        if (!StringUtils.hasText(allowBase)) {
            throw new BusinessException(ResultCode.ERROR, "未配置 public-base-url，无法代理");
        }
        URI allowUri = URI.create(allowBase);
        String allowHost = allowUri.getHost();
        int allowPort = allowUri.getPort();
        if (!StringUtils.hasText(allowHost)) {
            throw new BusinessException(ResultCode.ERROR, "public-base-url 配置不正确");
        }
        boolean hostOk = allowHost.equalsIgnoreCase(target.getHost());
        int targetPort = target.getPort();
        boolean portOk = allowPort == -1 ? (targetPort == -1 || targetPort == 80 || targetPort == 443) : allowPort == (targetPort == -1 ? allowPort : targetPort);
        if (!hostOk || !portOk) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不允许代理到该主机");
        }
    }

    private String resolveObjectName(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        String decoded = decodeUntilStable(rawPath);
        String normalized = decoded.startsWith("/") ? decoded.substring(1) : decoded;

        String basePath = null;
        try {
            URI allowUri = URI.create(minioConfig.getPublicBaseUrl());
            String allowPath = allowUri.getPath();
            if (allowPath != null) {
                basePath = decodeUntilStable(allowPath);
            }
        } catch (Exception ignored) {
        }
        if (basePath != null && !basePath.isBlank()) {
            String bp = basePath.startsWith("/") ? basePath.substring(1) : basePath;
            if (normalized.startsWith(bp)) {
                normalized = normalized.substring(bp.length());
                if (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
            }
        }

        String bucket = minioConfig.getBucket();
        if (!StringUtils.hasText(bucket)) {
            return null;
        }
        if (normalized.startsWith(bucket + "/")) {
            return normalized.substring(bucket.length() + 1);
        } else if (normalized.equals(bucket)) {
            return "";
        }
        return normalized;
    }

    private String decodeUntilStable(String value) {
        String current = value;
        while (current.contains("%")) {
            String next = URLDecoder.decode(current, StandardCharsets.UTF_8);
            if (next.equals(current)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private RangeSpec parseRange(String header, long totalSize) {
        if (!StringUtils.hasText(header) || !header.startsWith("bytes=") || totalSize <= 0) {
            return RangeSpec.full(totalSize);
        }
        String spec = header.substring(6).trim();
        if (spec.contains(",")) {
            return RangeSpec.full(totalSize);
        }
        String[] parts = spec.split("-", 2);
        try {
            String startPart = parts[0];
            String endPart = parts.length > 1 ? parts[1] : "";
            long start;
            long end;
            if (!startPart.isEmpty()) {
                start = Long.parseLong(startPart);
                if (start >= totalSize || start < 0) {
                    return RangeSpec.full(totalSize);
                }
                if (!endPart.isEmpty()) {
                    end = Long.parseLong(endPart);
                    if (end >= totalSize) {
                        end = totalSize - 1;
                    }
                    if (end < start) {
                        return RangeSpec.full(totalSize);
                    }
                } else {
                    end = totalSize - 1;
                }
            } else {
                long suffixLength = Long.parseLong(endPart);
                if (suffixLength <= 0) {
                    return RangeSpec.full(totalSize);
                }
                if (suffixLength >= totalSize) {
                    return RangeSpec.full(totalSize);
                }
                start = totalSize - suffixLength;
                end = totalSize - 1;
            }
            long length = end - start + 1;
            return RangeSpec.partial(start, end, length);
        } catch (NumberFormatException ex) {
            return RangeSpec.full(totalSize);
        }
    }

    private record RangeSpec(long offset, Long length, long end, boolean partial) {
        static RangeSpec full(long totalSize) {
            return new RangeSpec(0, null, totalSize > 0 ? totalSize - 1 : -1, false);
        }

        static RangeSpec partial(long start, long end, long length) {
            return new RangeSpec(start, length, end, true);
        }
    }
}
