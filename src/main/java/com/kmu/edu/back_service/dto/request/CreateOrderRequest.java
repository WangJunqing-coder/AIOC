package com.kmu.edu.back_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 创建订单请求
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {
    
    @NotNull(message = "产品类型不能为空")
    @Schema(description = "产品类型:1tokens,2VIP会员,3超级VIP", example = "1")
    private Integer productType;
    
    @NotNull(message = "产品数量不能为空")
    @Positive(message = "产品数量必须大于0")
    @Schema(description = "产品数量", example = "1000")
    private Integer quantity;
    
    @Schema(description = "备注", example = "购买1000个tokens")
    private String remark;
}