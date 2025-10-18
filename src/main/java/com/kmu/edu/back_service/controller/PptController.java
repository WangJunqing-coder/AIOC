package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.request.PptGenerationRequest;
import com.kmu.edu.back_service.dto.response.PptGenerationResponse;
import com.kmu.edu.back_service.dto.response.PptTemplateResponse;
import com.kmu.edu.back_service.service.PptGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ppt")
@Validated
@Tag(name = "PPT 生成")
public class PptController {

    private final PptGenerationService pptService;

    public PptController(PptGenerationService pptService) {
        this.pptService = pptService;
    }

    @PostMapping("/generate")
    @Operation(summary = "提交 PPT 生成任务")
    @SaCheckLogin
    public Result<PptGenerationResponse> generate(@Valid @RequestBody PptGenerationRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(pptService.generatePpt(userId, request));
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "查询生成状态")
    @SaCheckLogin
    public Result<PptGenerationResponse> status(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(pptService.getGenerationStatus(userId, id));
    }

    @GetMapping("/history")
    @Operation(summary = "获取用户生成历史")
    @SaCheckLogin
    public Result<List<PptGenerationResponse>> history(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                       @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(pptService.getUserGenerationHistory(userId, page, size));
    }

    @GetMapping("/templates")
    @Operation(summary = "获取可用模板")
    public Result<List<PptTemplateResponse>> templates() {
        return Result.success(pptService.getAvailableTemplates());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除记录")
    @SaCheckLogin
    public Result<Void> remove(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        pptService.deleteGeneration(userId, id);
        return Result.success();
    }
}
