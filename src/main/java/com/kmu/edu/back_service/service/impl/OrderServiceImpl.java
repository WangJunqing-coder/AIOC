package com.kmu.edu.back_service.service.impl;

import com.kmu.edu.back_service.dto.request.CreateOrderRequest;
import com.kmu.edu.back_service.dto.request.PayOrderRequest;
import com.kmu.edu.back_service.dto.response.OrderResponse;
import com.kmu.edu.back_service.dto.response.PaymentResponse;
import com.kmu.edu.back_service.entity.Order;
import com.kmu.edu.back_service.entity.SysUser;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.mapper.OrderMapper;
import com.kmu.edu.back_service.mapper.SysUserMapper;
import com.kmu.edu.back_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderMapper orderMapper;
    private final SysUserMapper sysUserMapper;
    
    // 订单号生成器
    private static final AtomicLong ORDER_COUNTER = new AtomicLong(1);
    
    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("创建订单，用户ID: {}, 产品类型: {}, 数量: {}", userId, request.getProductType(), request.getQuantity());
        
        // 验证用户
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 生成订单号
        String orderNo = generateOrderNo();
        
        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductType(request.getProductType());
        order.setPaymentStatus(0); // 待支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setRemark(request.getRemark());
        
        // 根据产品类型设置订单信息
        setOrderProductInfo(order, request.getProductType(), request.getQuantity());
        
        orderMapper.insert(order);
        
        log.info("订单创建成功，订单号: {}", orderNo);
        return convertToResponse(order);
    }
    
    @Override
    @Transactional
    public PaymentResponse payOrder(Long userId, PayOrderRequest request) {
        log.info("支付订单，用户ID: {}, 订单号: {}, 支付方式: {}", userId, request.getOrderNo(), request.getPaymentMethod());
        
        // 查询订单
        Order order = orderMapper.selectByOrderNo(request.getOrderNo());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        if (order.getPaymentStatus() != 0) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setOrderNo(request.getOrderNo());
        
        // 根据支付方式处理
        switch (request.getPaymentMethod()) {
            case 1: // 微信支付
                response = handleWechatPay(order);
                break;
            case 2: // 支付宝支付
                response = handleAlipay(order);
                break;
            case 3: // 余额支付
                response = handleBalancePay(order, userId);
                break;
            default:
                throw new BusinessException("不支持的支付方式");
        }
        
        // 更新订单支付方式
        order.setPaymentMethod(request.getPaymentMethod());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        return response;
    }
    
    @Override
    public OrderResponse getOrderDetail(Long userId, String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        return convertToResponse(order);
    }
    
    @Override
    public List<OrderResponse> getUserOrders(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Order> orders = orderMapper.selectByUserId(userId, offset, size);
        
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        if (order.getPaymentStatus() != 0) {
            throw new BusinessException("订单状态异常，无法取消");
        }
        
        order.setPaymentStatus(2); // 已取消
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("订单取消成功，订单号: {}", orderNo);
    }
    
    @Override
    public PaymentResponse getPaymentStatus(Long userId, String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setOrderNo(orderNo);
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentStatusDesc(getPaymentStatusDesc(order.getPaymentStatus()));
        
        return response;
    }
    
    @Override
    @Transactional
    public void handlePaymentCallback(String orderNo, String transactionId, Integer paymentMethod) {
        log.info("处理支付回调，订单号: {}, 交易ID: {}, 支付方式: {}", orderNo, transactionId, paymentMethod);
        
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("支付回调失败：订单不存在，订单号: {}", orderNo);
            return;
        }
        
        if (order.getPaymentStatus() == 1) {
            log.warn("订单已支付，忽略回调，订单号: {}", orderNo);
            return;
        }
        
        // 更新订单状态
        order.setPaymentStatus(1); // 已支付
        order.setPaymentTime(LocalDateTime.now());
        order.setTransactionId(transactionId);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 处理用户账户
        processUserAccount(order);
        
        log.info("支付回调处理完成，订单号: {}", orderNo);
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = ORDER_COUNTER.getAndIncrement() % 10000;
        return String.format("ORD%s%04d", date, sequence);
    }
    
    /**
     * 设置订单产品信息
     */
    private void setOrderProductInfo(Order order, Integer productType, Integer quantity) {
        switch (productType) {
            case 1: // tokens
                order.setProductName(quantity + "个Tokens");
                order.setAmount(new BigDecimal(quantity).multiply(new BigDecimal("0.01"))); // 1个token = 0.01元
                order.setTokensAmount(quantity);
                break;
            case 2: // VIP会员
                if (quantity == 30) {
                    order.setProductName("VIP会员 30天");
                    order.setAmount(new BigDecimal("29.9"));
                } else if (quantity == 365) {
                    order.setProductName("VIP会员 365天");
                    order.setAmount(new BigDecimal("299"));
                } else {
                    throw new BusinessException("不支持的VIP天数");
                }
                order.setVipDays(quantity);
                break;
            case 3: // 超级VIP
                if (quantity == 30) {
                    order.setProductName("超级VIP 30天");
                    order.setAmount(new BigDecimal("59.9"));
                } else if (quantity == 365) {
                    order.setProductName("超级VIP 365天");
                    order.setAmount(new BigDecimal("599"));
                } else {
                    throw new BusinessException("不支持的超级VIP天数");
                }
                order.setVipDays(quantity);
                break;
            default:
                throw new BusinessException("不支持的产品类型");
        }
    }
    
    /**
     * 处理微信支付
     */
    private PaymentResponse handleWechatPay(Order order) {
        // 这里应该调用微信支付API
        // 由于是示例，我们模拟返回支付链接
        PaymentResponse response = new PaymentResponse();
        response.setOrderNo(order.getOrderNo());
        response.setPaymentStatus(0); // 待支付
        response.setPaymentStatusDesc("待支付");
        response.setPaymentUrl("https://pay.weixin.qq.com/mock/" + order.getOrderNo());
        response.setMessage("请使用微信扫码支付");
        
        return response;
    }
    
    /**
     * 处理支付宝支付
     */
    private PaymentResponse handleAlipay(Order order) {
        // 这里应该调用支付宝API
        // 由于是示例，我们模拟返回支付链接
        PaymentResponse response = new PaymentResponse();
        response.setOrderNo(order.getOrderNo());
        response.setPaymentStatus(0); // 待支付
        response.setPaymentStatusDesc("待支付");
        response.setPaymentUrl("https://openapi.alipay.com/mock/" + order.getOrderNo());
        response.setMessage("请使用支付宝扫码支付");
        
        return response;
    }
    
    /**
     * 处理余额支付
     */
    @Transactional
    private PaymentResponse handleBalancePay(Order order, Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user.getBalance().compareTo(order.getAmount()) < 0) {
            throw new BusinessException("余额不足");
        }
        
        // 扣减余额
        sysUserMapper.updateBalance(userId, order.getAmount().negate().doubleValue());
        
        // 直接标记为已支付
        order.setPaymentStatus(1);
        order.setPaymentTime(LocalDateTime.now());
        order.setTransactionId("BALANCE_" + order.getOrderNo());
        orderMapper.updateById(order);
        
        // 处理用户账户
        processUserAccount(order);
        
        PaymentResponse response = new PaymentResponse();
        response.setOrderNo(order.getOrderNo());
        response.setPaymentStatus(1); // 已支付
        response.setPaymentStatusDesc("支付成功");
        response.setMessage("余额支付成功");
        
        return response;
    }
    
    /**
     * 处理用户账户（增加tokens或VIP）
     */
    private void processUserAccount(Order order) {
    sysUserMapper.selectById(order.getUserId());
        
        switch (order.getProductType()) {
            case 1: // tokens
                sysUserMapper.addTokens(order.getUserId(), order.getTokensAmount());
                break;
            case 2: // VIP会员
                sysUserMapper.updateUserType(order.getUserId(), 1);
                // 设置VIP过期时间
                LocalDateTime vipExpire = LocalDateTime.now().plusDays(order.getVipDays());
                order.setExpireTime(vipExpire);
                break;
            case 3: // 超级VIP
                sysUserMapper.updateUserType(order.getUserId(), 2);
                // 设置超级VIP过期时间
                LocalDateTime superVipExpire = LocalDateTime.now().plusDays(order.getVipDays());
                order.setExpireTime(superVipExpire);
                break;
        }
        
        orderMapper.updateById(order);
        
        log.info("用户账户处理完成，用户ID: {}, 产品类型: {}", order.getUserId(), order.getProductType());
    }
    
    /**
     * 转换为响应对象
     */
    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        BeanUtils.copyProperties(order, response);
        
        // 设置描述信息
        response.setProductTypeDesc(getProductTypeDesc(order.getProductType()));
        response.setPaymentMethodDesc(getPaymentMethodDesc(order.getPaymentMethod()));
        response.setPaymentStatusDesc(getPaymentStatusDesc(order.getPaymentStatus()));
        
        return response;
    }
    
    private String getProductTypeDesc(Integer productType) {
        if (productType == null) return null;
        switch (productType) {
            case 1: return "Tokens";
            case 2: return "VIP会员";
            case 3: return "超级VIP";
            default: return "未知";
        }
    }
    
    private String getPaymentMethodDesc(Integer paymentMethod) {
        if (paymentMethod == null) return null;
        switch (paymentMethod) {
            case 1: return "微信支付";
            case 2: return "支付宝支付";
            case 3: return "余额支付";
            default: return "未知";
        }
    }
    
    private String getPaymentStatusDesc(Integer paymentStatus) {
        if (paymentStatus == null) return null;
        switch (paymentStatus) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已取消";
            case 3: return "已退款";
            default: return "未知";
        }
    }
}