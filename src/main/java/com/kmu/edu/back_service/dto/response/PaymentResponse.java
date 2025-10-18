package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付响应
 */
@Data
@Schema(description = "支付响应")
public class PaymentResponse {
    
    @Schema(description = "订单号")
    private String orderNo;
    
    @Schema(description = "支付状态:0待支付,1已支付,2已取消,3已退款")
    private Integer paymentStatus;
    
    @Schema(description = "支付状态描述")
    private String paymentStatusDesc;
    
    @Schema(description = "支付链接或二维码(用于第三方支付)")
    private String paymentUrl;
    
    @Schema(description = "支付参数(用于客户端支付)")
    private String paymentParams;
    
    @Schema(description = "消息")
    private String message;
}