package com.kmu.edu.back_service.common;

/**
 * 响应状态码枚举
 */
public enum ResultCode {
    
    // 通用
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // 用户相关
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    USERNAME_EXIST(1003, "用户名已存在"),
    EMAIL_EXIST(1004, "邮箱已存在"),
    PHONE_EXIST(1005, "手机号已存在"),
    PASSWORD_ERROR(1006, "密码错误"),
    OLD_PASSWORD_ERROR(1007, "原密码错误"),
    
    // Token相关
    TOKEN_EXPIRED(2001, "Token已过期"),
    TOKEN_INVALID(2002, "Token无效"),
    TOKEN_NOT_FOUND(2003, "Token不存在"),
    
    // 业务相关
    INSUFFICIENT_BALANCE(3001, "余额不足"),
    INSUFFICIENT_TOKENS(3002, "Token数量不足"),
    DAILY_LIMIT_EXCEEDED(3003, "今日使用次数已达上限"),
    GENERATION_FAILED(3004, "生成失败"),
    FILE_UPLOAD_FAILED(3005, "文件上传失败"),
    
    // AI服务相关
    AI_SERVICE_ERROR(4001, "AI服务异常"),
    AI_SERVICE_TIMEOUT(4002, "AI服务超时"),
    CHAT_SESSION_NOT_FOUND(4003, "聊天会话不存在"),
    
    // 订单相关
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_PAID(5002, "订单已支付"),
    ORDER_CANCELLED(5003, "订单已取消"),
    PAYMENT_FAILED(5004, "支付失败");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}