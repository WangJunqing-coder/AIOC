package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 支付订单请求
 */
@Data
@Schema(description = "支付订单请求")
public class PayOrderRequest {
    
    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", example = "ORD202509190001")
    private String orderNo;
    
    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式:1微信,2支付宝,3余额", example = "1")
    private Integer paymentMethod;
}