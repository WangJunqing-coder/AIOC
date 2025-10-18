package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.github.pagehelper.PageInfo;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.request.ImageGenerationRequest;
import com.kmu.edu.back_service.dto.response.ImageGenerationResponse;
import com.kmu.edu.back_service.entity.ImageGeneration;
import com.kmu.edu.back_service.service.ImageGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/image")
@Validated
@Tag(name = "图片生成")
public class ImageController {

    private final ImageGenerationService imageService;

    public ImageController(ImageGenerationService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/generate")
    @Operation(summary = "提交图片生成任务")
    @SaCheckLogin
    public Result<ImageGenerationResponse> generate(@Valid @RequestBody ImageGenerationRequest request) {
        return Result.success(imageService.generateImage(request));
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "查询生成状态")
    @SaCheckLogin
    public Result<ImageGenerationResponse> status(@PathVariable("id") Long id) {
        return Result.success(imageService.getGenerationStatus(id));
    }

    @GetMapping("/history")
    @Operation(summary = "获取用户生成历史")
    @SaCheckLogin
    public Result<PageInfo<ImageGeneration>> history(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                     @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(imageService.getUserGenerations(userId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除生成记录")
    @SaCheckLogin
    public Result<Void> remove(@PathVariable("id") Long id) {
        imageService.deleteGeneration(id);
        return Result.success();
    }
}
