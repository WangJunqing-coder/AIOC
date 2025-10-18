package com.kmu.edu.back_service.service;

import com.kmu.edu.back_service.dto.request.UserLoginRequest;
import com.kmu.edu.back_service.dto.request.UserRegisterRequest;
import com.kmu.edu.back_service.dto.response.UserInfoResponse;
import com.kmu.edu.back_service.entity.SysUser;

/**
 * 用户服务接口
 */
public interface SysUserService {
    
    /**
     * 用户注册
     */
    void register(UserRegisterRequest request);
    
    /**
     * 用户登录
     */
    String login(UserLoginRequest request);
    
    /**
     * 用户登出
     */
    void logout();
    
    /**
     * 获取当前用户信息
     */
    UserInfoResponse getCurrentUserInfo();
    
    /**
     * 根据ID获取用户信息
     */
    SysUser getUserById(Long id);
    
    /**
     * 根据用户名获取用户
     */
    SysUser getUserByUsername(String username);
    
    /**
     * 更新用户信息
     */
    void updateUserInfo(SysUser user);
    
    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);
    
    /**
     * 重置密码
     */
    void resetPassword(String email, String newPassword);
    
    /**
     * 充值
     */
    void recharge(Long userId, Double amount);
    
    /**
     * 增加tokens
     */
    void addTokens(Long userId, Integer tokens);
    
    /**
     * 使用tokens
     */
    boolean useTokens(Long userId, Integer tokens);
    
    /**
     * 获取今日使用次数
     */
    Integer getTodayUsage(Long userId);
    
    /**
     * 检查今日使用限制
     */
    boolean checkDailyLimit(Long userId, Integer serviceType);
    
    /**
     * 升级用户类型
     */
    void upgradeUserType(Long userId, Integer userType, Integer days);
}