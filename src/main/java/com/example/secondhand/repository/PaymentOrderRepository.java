package com.example.secondhand.repository;

import com.example.secondhand.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付订单数据访问接口
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, String> {

    /**
     * 根据支付宝交易号查询支付订单
     * @param alipayTradeNo 支付宝交易号
     * @return 支付订单
     */
    Optional<PaymentOrder> findByAlipayTradeNo(String alipayTradeNo);

    /**
     * 根据买家ID查询支付订单列表
     * @param buyerId 买家ID
     * @return 支付订单列表
     */
    List<PaymentOrder> findByBuyerIdOrderByCreateTimeDesc(Long buyerId);

    /**
     * 根据买家ID和支付状态查询支付订单列表
     * @param buyerId 买家ID
     * @param status 支付状态
     * @return 支付订单列表
     */
    List<PaymentOrder> findByBuyerIdAndStatusOrderByCreateTimeDesc(Long buyerId, PaymentOrder.PaymentStatus status);

    /**
     * 根据支付状态查询支付订单列表
     * @param status 支付状态
     * @return 支付订单列表
     */
    List<PaymentOrder> findByStatusOrderByCreateTimeDesc(PaymentOrder.PaymentStatus status);

    /**
     * 查询指定时间范围内的支付订单
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付订单列表
     */
    List<PaymentOrder> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内指定状态的支付订单
     * @param status 支付状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付订单列表
     */
    List<PaymentOrder> findByStatusAndCreateTimeBetweenOrderByCreateTimeDesc(
            PaymentOrder.PaymentStatus status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询超时的待支付订单
     * @param timeoutTime 超时时间点
     * @return 超时订单列表
     */
    @Query("SELECT p FROM PaymentOrder p WHERE p.status = :status AND p.createTime < :timeoutTime")
    List<PaymentOrder> findTimeoutOrders(@Param("status") PaymentOrder.PaymentStatus status, 
                                        @Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 查询指定时间之前创建的待支付订单
     * @param createTimeBefore 创建时间上限
     * @return 待支付订单列表
     */
    List<PaymentOrder> findByStatusAndCreateTimeBeforeOrderByCreateTimeAsc(
            PaymentOrder.PaymentStatus status, LocalDateTime createTimeBefore);

    /**
     * 统计买家的支付订单数量
     * @param buyerId 买家ID
     * @return 订单数量
     */
    long countByBuyerId(Long buyerId);

    /**
     * 统计买家指定状态的支付订单数量
     * @param buyerId 买家ID
     * @param status 支付状态
     * @return 订单数量
     */
    long countByBuyerIdAndStatus(Long buyerId, PaymentOrder.PaymentStatus status);

    /**
     * 统计指定状态的支付订单数量
     * @param status 支付状态
     * @return 订单数量
     */
    long countByStatus(PaymentOrder.PaymentStatus status);

    /**
     * 统计指定时间范围内的支付订单数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单数量
     */
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定时间范围内指定状态的支付订单数量
     * @param status 支付状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单数量
     */
    long countByStatusAndCreateTimeBetween(PaymentOrder.PaymentStatus status, 
                                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 检查订单是否属于指定买家
     * @param orderNo 订单号
     * @param buyerId 买家ID
     * @return 是否存在
     */
    boolean existsByOrderNoAndBuyerId(String orderNo, Long buyerId);

    /**
     * 检查支付宝交易号是否已存在
     * @param alipayTradeNo 支付宝交易号
     * @return 是否存在
     */
    boolean existsByAlipayTradeNo(String alipayTradeNo);

    /**
     * 查询买家在指定时间范围内的支付订单
     * @param buyerId 买家ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付订单列表
     */
    List<PaymentOrder> findByBuyerIdAndCreateTimeBetweenOrderByCreateTimeDesc(
            Long buyerId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询买家在指定时间范围内指定状态的支付订单
     * @param buyerId 买家ID
     * @param status 支付状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付订单列表
     */
    List<PaymentOrder> findByBuyerIdAndStatusAndCreateTimeBetweenOrderByCreateTimeDesc(
            Long buyerId, PaymentOrder.PaymentStatus status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近的支付订单（用于统计分析）
     * @param pageable 分页参数
     * @return 支付订单列表
     */
    @Query("SELECT p FROM PaymentOrder p ORDER BY p.createTime DESC")
    List<PaymentOrder> findRecentOrders(org.springframework.data.domain.Pageable pageable);

    /**
     * 查询支付成功率统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果 [总数, 成功数]
     */
    @Query("SELECT COUNT(p), SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END) " +
           "FROM PaymentOrder p WHERE p.createTime BETWEEN :startTime AND :endTime")
    Object[] getPaymentSuccessRate(@Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均支付时长（分钟）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均支付时长
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, p.createTime, p.payTime)) " +
           "FROM PaymentOrder p WHERE p.status = 'PAID' AND p.payTime IS NOT NULL " +
           "AND p.createTime BETWEEN :startTime AND :endTime")
    Double getAveragePaymentDuration(@Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime);
}