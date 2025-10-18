package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.request.VideoGenerationRequest;
import com.kmu.edu.back_service.dto.response.VideoGenerationResponse;
import com.kmu.edu.back_service.entity.VideoGeneration;
import com.kmu.edu.back_service.service.VideoGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/video")
@Validated
@Tag(name = "视频生成")
public class VideoController {

    private final VideoGenerationService videoService;

    public VideoController(VideoGenerationService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/generate")
    @Operation(summary = "提交视频生成任务")
    @SaCheckLogin
    public Result<VideoGenerationResponse> generate(@Valid @RequestBody VideoGenerationRequest request) {
        return Result.success(videoService.generateVideo(request));
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "查询生成状态")
    @SaCheckLogin
    public Result<VideoGenerationResponse> status(@PathVariable("id") Long id) {
        return Result.success(videoService.getGenerationStatus(id));
    }

    @GetMapping("/history")
    @Operation(summary = "获取用户生成历史")
    @SaCheckLogin
    public Result<List<VideoGeneration>> history(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                 @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(videoService.getUserGenerations(userId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除生成记录")
    @SaCheckLogin
    public Result<Void> remove(@PathVariable("id") Long id) {
        videoService.deleteGeneration(id);
        return Result.success();
    }
}
