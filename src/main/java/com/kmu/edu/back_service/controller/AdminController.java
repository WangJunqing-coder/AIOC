package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.common.ResultCode;
import com.kmu.edu.back_service.entity.PptTemplate;
import com.kmu.edu.back_service.entity.SysConfig;
import com.kmu.edu.back_service.entity.SysUser;
import com.kmu.edu.back_service.entity.Order;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.mapper.OrderMapper;
import com.kmu.edu.back_service.mapper.PptTemplateMapper;
import com.kmu.edu.back_service.mapper.SysConfigMapper;
import com.kmu.edu.back_service.mapper.SysUserMapper;
import com.kmu.edu.back_service.service.MinioStorageService;
import com.kmu.edu.back_service.service.SysUserService;
import com.kmu.edu.back_service.utils.CommonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Validated
@Tag(name = "后台管理")
public class AdminController {

    private final SysUserService userService;
    private final SysUserMapper userMapper;
    private final OrderMapper orderMapper;
    private final SysConfigMapper configMapper;
    private final PptTemplateMapper pptTemplateMapper;
    private final MinioStorageService storageService;

    public AdminController(SysUserService userService,
                           SysUserMapper userMapper,
                           OrderMapper orderMapper,
                           SysConfigMapper configMapper,
                           PptTemplateMapper pptTemplateMapper,
                           MinioStorageService storageService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.configMapper = configMapper;
        this.pptTemplateMapper = pptTemplateMapper;
        this.storageService = storageService;
    }

    private void ensureAdmin() {
        // 优先使用 Sa-Token 角色检查，如未配置则回退到用户信息判断
        try {
            if (StpUtil.hasRole("ADMIN")) return;
        } catch (Exception ignored) {}
        var info = userService.getCurrentUserInfo();
        if (info == null || !"ADMIN".equalsIgnoreCase(info.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可访问");
        }
    }

    // 概览
    @GetMapping("/summary")
    @SaCheckRole("ADMIN")
    @Operation(summary = "平台概览")
    public Result<Map<String, Object>> summary() {
        ensureAdmin();
        long users = userMapper.selectCount(null, null, null, null);
        long orders = orderMapper.countAll();
        BigDecimal paidAmount = orderMapper.sumPaidAmount();
        return Result.success(Map.of(
                "users", users,
                "orders", orders,
                "paidAmount", paidAmount
        ));
    }

    // 统计（占位）
    @GetMapping("/analytics/pv")
    @SaCheckRole("ADMIN")
    public Result<List<Map<String, Object>>> pv(@RequestParam(defaultValue = "7") Integer days) {
        ensureAdmin();
        return Result.success(List.of());
    }

    @GetMapping("/analytics/traffic")
    @SaCheckRole("ADMIN")
    public Result<List<Map<String, Object>>> traffic(@RequestParam(defaultValue = "7") Integer days) {
        ensureAdmin();
        return Result.success(List.of());
    }

    // 用户管理
    @GetMapping("/users")
    @SaCheckRole("ADMIN")
    public Result<Map<String, Object>> userPage(@RequestParam(required = false) String username,
                                                @RequestParam(required = false) String email,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "20") Integer size) {
        ensureAdmin();
        int offset = CommonUtils.calculateOffset(page, size);
        List<SysUser> list = userMapper.selectPage(username, email, role, status, offset, size);
        long total = userMapper.selectCount(username, email, role, status);
        return Result.success(Map.of("list", list, "total", total));
    }

    @PutMapping("/users/{id}/status")
    @SaCheckRole("ADMIN")
    public Result<Void> userUpdateStatus(@PathVariable Long id, @RequestParam Integer status) {
        ensureAdmin();
        userMapper.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/users/{id}/type")
    @SaCheckRole("ADMIN")
    public Result<Void> userUpdateType(@PathVariable Long id, @RequestParam Integer userType) {
        ensureAdmin();
        userMapper.updateUserType(id, userType);
        return Result.success();
    }

    @PutMapping("/users/{id}/role")
    @SaCheckRole("ADMIN")
    public Result<Void> userUpdateRole(@PathVariable Long id, @RequestParam String role) {
        ensureAdmin();
        userMapper.updateRole(id, role);
        return Result.success();
    }

    @PostMapping("/users/{id}/recharge")
    @SaCheckRole("ADMIN")
    public Result<Void> userRecharge(@PathVariable Long id, @RequestParam Double amount) {
        ensureAdmin();
        userMapper.updateBalance(id, amount);
        return Result.success();
    }

    @PostMapping("/users/{id}/tokens")
    @SaCheckRole("ADMIN")
    public Result<Void> userAddTokens(@PathVariable Long id, @RequestParam Integer tokens) {
        ensureAdmin();
        userMapper.addTokens(id, tokens);
        return Result.success();
    }

    @PostMapping("/users/{id}/reset-password")
    @SaCheckRole("ADMIN")
    public Result<Void> userResetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        ensureAdmin();
        String enc = CommonUtils.encryptPassword(newPassword);
        userMapper.updatePassword(id, enc);
        return Result.success();
    }

    // 订单管理
    @GetMapping("/orders")
    @SaCheckRole("ADMIN")
    public Result<Map<String, Object>> orderPage(@RequestParam(required = false) Integer paymentStatus,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer size) {
        ensureAdmin();
        int offset = CommonUtils.calculateOffset(page, size);
        List<Order> list;
        long total;
        if (paymentStatus != null) {
            list = orderMapper.selectByPaymentStatus(paymentStatus, offset, size);
            total = orderMapper.countByPaymentStatus(paymentStatus);
        } else {
            list = orderMapper.selectAll(offset, size);
            total = orderMapper.countAll();
        }
        return Result.success(Map.of("list", list, "total", total));
    }

    @PostMapping("/orders/{id}/refund")
    @SaCheckRole("ADMIN")
    public Result<Void> orderRefund(@PathVariable Long id) {
        ensureAdmin();
        // 这里只做占位，真实退款逻辑需接入支付网关并回写状态
        return Result.success();
    }

    // 系统配置
    @GetMapping("/configs")
    @SaCheckRole("ADMIN")
    public Result<Map<String, Object>> configPage(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer size) {
        ensureAdmin();
        int offset = CommonUtils.calculateOffset(page, size);
        List<SysConfig> list = configMapper.selectPage(offset, size);
        long total = configMapper.countAll();
        return Result.success(Map.of("list", list, "total", total));
    }

    @PostMapping("/configs")
    @SaCheckRole("ADMIN")
    public Result<Void> configCreate(@RequestBody SysConfig cfg) {
        ensureAdmin();
        configMapper.insert(cfg);
        return Result.success();
    }

    @PutMapping("/configs/{id}")
    @SaCheckRole("ADMIN")
    public Result<Void> configUpdate(@PathVariable Long id, @RequestBody SysConfig cfg) {
        ensureAdmin();
        cfg.setId(id);
        configMapper.updateById(cfg);
        return Result.success();
    }

    @DeleteMapping("/configs/{id}")
    @SaCheckRole("ADMIN")
    public Result<Void> configDelete(@PathVariable Long id) {
        ensureAdmin();
        configMapper.deleteById(id);
        return Result.success();
    }

    // PPT 模板
    @GetMapping("/ppt-templates")
    @SaCheckRole("ADMIN")
    public Result<Map<String, Object>> pptTemplatePage(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer size) {
        ensureAdmin();
        int offset = CommonUtils.calculateOffset(page, size);
        List<PptTemplate> list = pptTemplateMapper.selectAll(offset, size);
        long total = pptTemplateMapper.countAll();
        return Result.success(Map.of("list", list, "total", total));
    }

    @PostMapping("/ppt-templates")
    @SaCheckRole("ADMIN")
    public Result<Void> pptTemplateCreate(@RequestBody PptTemplate t) {
        ensureAdmin();
        pptTemplateMapper.insert(t);
        return Result.success();
    }

    @PutMapping("/ppt-templates/{id}")
    @SaCheckRole("ADMIN")
    public Result<Void> pptTemplateUpdate(@PathVariable Long id, @RequestBody PptTemplate t) {
        ensureAdmin();
        t.setId(id);
        pptTemplateMapper.updateById(t);
        return Result.success();
    }

    @DeleteMapping("/ppt-templates/{id}")
    @SaCheckRole("ADMIN")
    public Result<Void> pptTemplateDelete(@PathVariable Long id) {
        ensureAdmin();
        pptTemplateMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/ppt-templates/upload")
    @SaCheckRole("ADMIN")
    public Result<Map<String, String>> uploadTemplate(@RequestPart("templateFile") MultipartFile templateFile,
                                                      @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {
        ensureAdmin();
        try {
            String tName = "ppt/templates/" + System.currentTimeMillis() + "-" + templateFile.getOriginalFilename();
            String tUrl = storageService.upload(tName, templateFile.getInputStream(), templateFile.getSize(), templateFile.getContentType());
            String thumbUrl = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String imgName = "ppt/templates/" + System.currentTimeMillis() + "-" + thumbnailFile.getOriginalFilename();
                thumbUrl = storageService.upload(imgName, thumbnailFile.getInputStream(), thumbnailFile.getSize(), thumbnailFile.getContentType());
            }
            return Result.success(Map.of("templateUrl", tUrl, "thumbnailUrl", thumbUrl));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.ERROR, "上传失败: " + e.getMessage());
        }
    }
}
