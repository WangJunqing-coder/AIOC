package com.kmu.edu.back_service.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 用户实体
 */
@Data
public class SysUser {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码(加密)
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 性别:0未知,1男,2女
     */
    private Integer gender;
    
    /**
     * 生日
     */
    private LocalDate birthday;
    
    /**
     * 用户类型:0普通用户,1VIP,2超级VIP
     */
    private Integer userType;
    
    /**
     * 角色: USER/ADMIN
     */
    private String role;

    /**
     * 状态:0禁用,1启用
     */
    private Integer status;
    
    /**
     * 余额
     */
    private BigDecimal balance;
    
    /**
     * 总token数
     */
    private Integer totalTokens;
    
    /**
     * 已使用token数
     */
    private Integer usedTokens;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}