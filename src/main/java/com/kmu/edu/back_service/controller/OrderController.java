package com.kmu.edu.back_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.kmu.edu.back_service.common.Result;
import com.kmu.edu.back_service.dto.request.CreateOrderRequest;
import com.kmu.edu.back_service.dto.request.PayOrderRequest;
import com.kmu.edu.back_service.dto.response.OrderResponse;
import com.kmu.edu.back_service.dto.response.PaymentResponse;
import com.kmu.edu.back_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@Validated
@Tag(name = "订单")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(orderService.createOrder(userId, request));
    }

    @PostMapping("/pay")
    @Operation(summary = "支付订单")
    public Result<PaymentResponse> pay(@Valid @RequestBody PayOrderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(orderService.payOrder(userId, request));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "订单详情")
    public Result<OrderResponse> detail(@PathVariable("orderNo") String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(orderService.getOrderDetail(userId, orderNo));
    }

    @GetMapping("/list")
    @Operation(summary = "订单列表")
    public Result<List<OrderResponse>> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(orderService.getUserOrders(userId, page, size));
    }
}
