package com.kmu.edu.back_service.controller;

import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.request.UserLoginRequest;
import com.kmu.edu.back_service.dto.request.UserRegisterRequest;
import com.kmu.edu.back_service.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "认证与账户")
public class AuthController {

    private final SysUserService sysUserService;

    public AuthController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        sysUserService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, String>> login(@Valid @RequestBody UserLoginRequest request) {
        String token = sysUserService.login(request);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return Result.success("登录成功", data);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        sysUserService.logout();
        return Result.success();
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@RequestParam("oldPassword") String oldPassword,
                                       @RequestParam("newPassword") String newPassword) {
        sysUserService.changePassword(oldPassword, newPassword);
        return Result.success();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码（通过邮箱）")
    public Result<Void> resetPassword(@RequestParam("email") String email,
                                      @RequestParam("newPassword") String newPassword) {
        sysUserService.resetPassword(email, newPassword);
        return Result.success();
    }
}
