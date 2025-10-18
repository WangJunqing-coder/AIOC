package com.kmu.edu.back_service.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 是否成功
     */
    private Boolean success;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
        this.success = ResultCode.SUCCESS.getCode().equals(code);
    }
    
    public Result(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }
    
    /**
     * 成功
     */
    public static <T> Result<T> success() {
        Result<T> r = new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
        r.setSuccess(true);
        return r;
    }
    
    /**
     * 成功
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
        r.setSuccess(true);
        return r;
    }
    
    /**
     * 成功
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>(ResultCode.SUCCESS.getCode(), message, data);
        r.setSuccess(true);
        return r;
    }
    
    /**
     * 失败
     */
    public static <T> Result<T> error() {
        Result<T> r = new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage());
        r.setSuccess(false);
        return r;
    }
    
    /**
     * 失败
     */
    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>(ResultCode.ERROR.getCode(), message);
        r.setSuccess(false);
        return r;
    }
    
    /**
     * 失败
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>(code, message);
        r.setSuccess(false);
        return r;
    }
    
    /**
     * 失败
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> r = new Result<>(resultCode.getCode(), resultCode.getMessage());
        r.setSuccess(false);
        return r;
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}