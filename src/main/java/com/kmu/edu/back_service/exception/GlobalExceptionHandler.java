package com.kmu.edu.back_service.exception;

import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.common.ResultCode;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 非法参数
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }
    
    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder sb = new StringBuilder();
        for (FieldError fieldError : fieldErrors) {
            sb.append(fieldError.getDefaultMessage()).append(";");
        }
        String errorMsg = sb.toString();
        if (errorMsg.endsWith(";")) {
            errorMsg = errorMsg.substring(0, errorMsg.length() - 1);
        }
        log.warn("参数校验失败: {}", errorMsg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMsg);
    }
    
    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder sb = new StringBuilder();
        for (FieldError fieldError : fieldErrors) {
            sb.append(fieldError.getDefaultMessage()).append(";");
        }
        String errorMsg = sb.toString();
        if (errorMsg.endsWith(";")) {
            errorMsg = errorMsg.substring(0, errorMsg.length() - 1);
        }
        log.warn("参数绑定失败: {}", errorMsg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMsg);
    }
    
    /**
     * 静态资源未找到异常处理
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // 对于常见的静态资源请求，只记录debug日志，不记录错误
        if (requestURI.equals("/favicon.ico") || 
            requestURI.startsWith("/hybridaction/") ||
            requestURI.equals("/robots.txt") ||
            requestURI.equals("/sitemap.xml")) {
            log.debug("静态资源未找到: {}", requestURI);
            return Result.error(ResultCode.NOT_FOUND.getCode(), "资源未找到");
        }
        
        log.warn("资源未找到: {} - {}", requestURI, e.getMessage());
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /**
     * 方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求方法不被支持: " + e.getMethod());
    }
    
    /**
     * 其他异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // 对于SpringDoc和Knife4j的路径，特殊处理
        if (requestURI.startsWith("/v3/api-docs") || 
            requestURI.startsWith("/swagger-ui") ||
            requestURI.startsWith("/webjars") ||
            requestURI.startsWith("/api-docs")) {
            // 对于SpringDoc相关错误，记录warn日志但不中断处理
            log.warn("SpringDoc/Knife4j相关异常: {} - {}", requestURI, e.getMessage());
            // 返回友好的错误信息
            return Result.error(ResultCode.ERROR.getCode(), "API文档服务不可用");
        }
        
        // 对于静态资源相关的异常，降低日志级别
        if (requestURI.equals("/favicon.ico") || 
            requestURI.startsWith("/hybridaction/") ||
            requestURI.equals("/robots.txt") ||
            requestURI.equals("/sitemap.xml")) {
            log.debug("静态资源异常: {} - {}", requestURI, e.getMessage());
            return Result.error("资源不可用");
        }
        
        log.error("系统异常: {} - {}", requestURI, e.getMessage(), e);
        return Result.error(ResultCode.ERROR.getCode(), "系统繁忙，请稍后再试");
    }
}