package com.kmu.edu.back_service.controller;

import com.kmu.edu.back_service.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.success(Map.of(
                "name", "AIOC",
                "time", OffsetDateTime.now().toString()
        ));
    }

    @GetMapping("/visit")
    public Result<Map<String, Object>> visit() {
        return Result.success(Map.of(
                "status", "success"
        ));
    }
}
