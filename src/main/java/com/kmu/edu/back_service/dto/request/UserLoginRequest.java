package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;



/**
 * 用户登录请求DTO
 */
@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest {
    
    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号(用户名/邮箱/手机号)", example = "testuser")
    private String account;
    
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}