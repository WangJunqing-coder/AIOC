package com.kmu.edu.back_service.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通用工具类
 */
@Slf4j
@Component
public class CommonUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 生成唯一ID
     */
    public static String generateId() {
        return IdUtil.simpleUUID();
    }
    
    /**
     * 生成订单号
     */
    public static String generateOrderNo() {
        return "AI" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase();
    }
    
    /**
     * 生成会话ID
     */
    public static String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + IdUtil.randomUUID().substring(0, 8);
    }
    
    /**
     * 密码加密
     */
    public static String encryptPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    /**
     * 密码验证
     */
    public static boolean checkPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }
    
    /**
     * 获取当前时间字符串
     */
    public static String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    /**
     * 生成文件名
     */
    public static String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return generateId() + (extension.isEmpty() ? "" : "." + extension);
    }
    
    /**
     * 对象转JSON字符串
     */
    public static String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON字符串失败", e);
            return "";
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public static <T> T fromJsonString(String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (Exception e) {
            log.error("JSON字符串转对象失败", e);
            return null;
        }
    }
    
    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 获取缓存Key
     */
    public static String getCacheKey(String prefix, Object... params) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object param : params) {
            sb.append(param).append(":");
        }
        return sb.toString();
    }
    
    /**
     * 计算分页偏移量
     */
    public static int calculateOffset(int page, int size) {
        return Math.max(0, (page - 1) * size);
    }
}