package com.kmu.edu.back_service.service;

import com.kmu.edu.back_service.dto.request.CreateOrderRequest;
import com.kmu.edu.back_service.dto.request.PayOrderRequest;
import com.kmu.edu.back_service.dto.response.OrderResponse;
import com.kmu.edu.back_service.dto.response.PaymentResponse;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     * 
     * @param userId 用户ID
     * @param request 创建订单请求
     * @return 订单响应
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);
    
    /**
     * 支付订单
     * 
     * @param userId 用户ID
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse payOrder(Long userId, PayOrderRequest request);
    
    /**
     * 查询订单详情
     * 
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderResponse getOrderDetail(Long userId, String orderNo);
    
    /**
     * 获取用户订单列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 订单列表
     */
    List<OrderResponse> getUserOrders(Long userId, Integer page, Integer size);
    
    /**
     * 取消订单
     * 
     * @param userId 用户ID
     * @param orderNo 订单号
     */
    void cancelOrder(Long userId, String orderNo);
    
    /**
     * 查询支付状态
     * 
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 支付状态
     */
    PaymentResponse getPaymentStatus(Long userId, String orderNo);
    
    /**
     * 处理支付回调
     * 
     * @param orderNo 订单号
     * @param transactionId 第三方交易ID
     * @param paymentMethod 支付方式
     */
    void handlePaymentCallback(String orderNo, String transactionId, Integer paymentMethod);
}