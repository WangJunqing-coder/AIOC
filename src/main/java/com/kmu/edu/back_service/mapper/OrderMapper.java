package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper {
    
    /**
     * 插入订单
     */
    @org.apache.ibatis.annotations.Insert("INSERT INTO orders (order_no, user_id, product_type, product_name, amount, payment_method, payment_status, payment_time, transaction_id, tokens_amount, vip_days, expire_time, remark, create_time, update_time) " +
        "VALUES (#{orderNo}, #{userId}, #{productType}, #{productName}, #{amount}, #{paymentMethod}, #{paymentStatus}, #{paymentTime}, #{transactionId}, #{tokensAmount}, #{vipDays}, #{expireTime}, #{remark}, NOW(), NOW())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);
    
    /**
     * 根据ID查询订单
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM orders WHERE id = #{id}")
    Order selectById(@Param("id") Long id);
    
    /**
     * 根据订单号查询订单
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Order selectByOrderNo(@Param("orderNo") String orderNo);
    
    /**
     * 根据用户ID查询订单列表
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Order> selectByUserId(@Param("userId") Long userId,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);
    
    /**
     * 统计用户订单数量
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM orders WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 更新订单
     */
    @org.apache.ibatis.annotations.Update({
        "<script>",
        "UPDATE orders",
        "<set>",
        "  <if test='orderNo != null'>order_no = #{orderNo},</if>",
        "  <if test='userId != null'>user_id = #{userId},</if>",
        "  <if test='productType != null'>product_type = #{productType},</if>",
        "  <if test='productName != null'>product_name = #{productName},</if>",
        "  <if test='amount != null'>amount = #{amount},</if>",
        "  <if test='paymentMethod != null'>payment_method = #{paymentMethod},</if>",
        "  <if test='paymentStatus != null'>payment_status = #{paymentStatus},</if>",
        "  <if test='paymentTime != null'>payment_time = #{paymentTime},</if>",
        "  <if test='transactionId != null'>transaction_id = #{transactionId},</if>",
        "  <if test='tokensAmount != null'>tokens_amount = #{tokensAmount},</if>",
        "  <if test='vipDays != null'>vip_days = #{vipDays},</if>",
        "  <if test='expireTime != null'>expire_time = #{expireTime},</if>",
        "  <if test='remark != null'>remark = #{remark},</if>",
        "  update_time = NOW()",
        "</set>",
        "WHERE id = #{id}",
        "</script>"
    })
    int updateById(Order order);
    
    /**
     * 根据ID删除订单
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM orders WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    /**
     * 查询所有订单（分页）
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM orders ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Order> selectAll(@Param("offset") Integer offset,
                         @Param("limit") Integer limit);
    
    /**
     * 统计总数量
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM orders")
    Long countAll();
    
    /**
     * 根据支付状态查询订单
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM orders WHERE payment_status = #{paymentStatus} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Order> selectByPaymentStatus(@Param("paymentStatus") Integer paymentStatus,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);

    /**
     * 按支付状态统计数量
     */
    @Select("SELECT COUNT(*) FROM orders WHERE payment_status = #{paymentStatus}")
    Long countByPaymentStatus(@Param("paymentStatus") Integer paymentStatus);

    /**
     * 统计已支付订单的总金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM orders WHERE payment_status = 1")
    java.math.BigDecimal sumPaidAmount();
}