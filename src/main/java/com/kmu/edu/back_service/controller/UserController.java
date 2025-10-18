package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.response.UserInfoResponse;
import com.kmu.edu.back_service.entity.SysUser;
import com.kmu.edu.back_service.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Validated
@Tag(name = "用户")
public class UserController {

    private final SysUserService sysUserService;

    public UserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    @SaCheckLogin
    public Result<UserInfoResponse> info() {
        return Result.success(sysUserService.getCurrentUserInfo());
    }

    @PutMapping("/info")
    @Operation(summary = "更新当前用户信息")
    @SaCheckLogin
    public Result<Void> update(@Valid @RequestBody SysUser user) {
        sysUserService.updateUserInfo(user);
        return Result.success();
    }

    @PostMapping("/recharge")
    @Operation(summary = "余额充值")
    @SaCheckLogin
    public Result<Void> recharge(@RequestParam("amount") Double amount) {
        // 这里内部会取当前登录用户ID
        UserInfoResponse cur = sysUserService.getCurrentUserInfo();
        sysUserService.recharge(cur.getId(), amount);
        return Result.success();
    }
}
