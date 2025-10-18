package com.kmu.edu.back_service.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
public class Order {
    
    /**
     * 订单ID
     */
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 产品类型:1tokens,2VIP会员,3超级VIP
     */
    private Integer productType;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 支付方式:1微信,2支付宝,3余额
     */
    private Integer paymentMethod;
    
    /**
     * 支付状态:0待支付,1已支付,2已取消,3已退款
     */
    private Integer paymentStatus;
    
    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;
    
    /**
     * 第三方交易ID
     */
    private String transactionId;
    
    /**
     * token数量
     */
    private Integer tokensAmount;
    
    /**
     * VIP天数
     */
    private Integer vipDays;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}