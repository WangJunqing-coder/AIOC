package com.kmu.edu.back_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应
 */
@Data
@Schema(description = "订单响应")
public class OrderResponse {
    
    @Schema(description = "订单ID")
    private Long id;
    
    @Schema(description = "订单号")
    private String orderNo;
    
    @Schema(description = "产品类型:1tokens,2VIP会员,3超级VIP")
    private Integer productType;
    
    @Schema(description = "产品类型描述")
    private String productTypeDesc;
    
    @Schema(description = "产品名称")
    private String productName;
    
    @Schema(description = "订单金额")
    private BigDecimal amount;
    
    @Schema(description = "支付方式:1微信,2支付宝,3余额")
    private Integer paymentMethod;
    
    @Schema(description = "支付方式描述")
    private String paymentMethodDesc;
    
    @Schema(description = "支付状态:0待支付,1已支付,2已取消,3已退款")
    private Integer paymentStatus;
    
    @Schema(description = "支付状态描述")
    private String paymentStatusDesc;
    
    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;
    
    @Schema(description = "第三方交易ID")
    private String transactionId;
    
    @Schema(description = "token数量")
    private Integer tokensAmount;
    
    @Schema(description = "VIP天数")
    private Integer vipDays;
    
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}