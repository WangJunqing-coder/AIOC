package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
@Schema(description = "用户信息")
public class UserInfoResponse {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "性别:0未知,1男,2女")
    private Integer gender;
    
    @Schema(description = "生日")
    private LocalDate birthday;
    
    @Schema(description = "用户类型:0普通用户,1VIP,2超级VIP")
    private Integer userType;
    
    @Schema(description = "用户类型名称")
    private String userTypeName;
    
    @Schema(description = "状态:0禁用,1启用")
    private Integer status;

    @Schema(description = "角色:USER/ADMIN")
    private String role;
    
    @Schema(description = "余额")
    private BigDecimal balance;
    
    @Schema(description = "总token数")
    private Integer totalTokens;
    
    @Schema(description = "已使用token数")
    private Integer usedTokens;
    
    @Schema(description = "剩余token数")
    private Integer remainingTokens;
    
    @Schema(description = "今日已使用次数")
    private Integer todayUsage;
    
    @Schema(description = "今日使用限制")
    private Integer dailyLimit;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}