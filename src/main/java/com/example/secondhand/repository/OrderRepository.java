package com.example.secondhand.repository;

import com.example.secondhand.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据买家ID查询订单列表（包含订单项）
     * @param buyerId 买家ID
     * @return 订单列表
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.buyerId = :buyerId ORDER BY o.createTime DESC")
    List<Order> findByBuyerIdOrderByCreateTimeDesc(@Param("buyerId") Long buyerId);

    /**
     * 根据买家ID和订单状态查询订单列表（包含订单项）
     * @param buyerId 买家ID
     * @param status 订单状态
     * @return 订单列表
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.buyerId = :buyerId AND o.status = :status ORDER BY o.createTime DESC")
    List<Order> findByBuyerIdAndStatusOrderByCreateTimeDesc(@Param("buyerId") Long buyerId, @Param("status") Order.OrderStatus status);

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 查询卖家的销售订单（通过订单项中的卖家ID，包含订单项）
     * @param sellerId 卖家ID
     * @return 订单列表
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items oi WHERE oi.sellerId = :sellerId ORDER BY o.createTime DESC")
    List<Order> findSellerOrders(@Param("sellerId") Long sellerId);

    /**
     * 根据卖家ID和订单状态查询销售订单（包含订单项）
     * @param sellerId 卖家ID
     * @param status 订单状态
     * @return 订单列表
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items oi WHERE oi.sellerId = :sellerId AND o.status = :status ORDER BY o.createTime DESC")
    List<Order> findSellerOrdersByStatus(@Param("sellerId") Long sellerId, @Param("status") Order.OrderStatus status);

    /**
     * 查询指定时间范围内的订单
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单列表
     */
    List<Order> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计买家的订单数量
     * @param buyerId 买家ID
     * @return 订单数量
     */
    long countByBuyerId(Long buyerId);

    /**
     * 统计买家指定状态的订单数量
     * @param buyerId 买家ID
     * @param status 订单状态
     * @return 订单数量
     */
    long countByBuyerIdAndStatus(Long buyerId, Order.OrderStatus status);

    /**
     * 统计卖家的销售订单数量
     * @param sellerId 卖家ID
     * @return 订单数量
     */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items oi WHERE oi.sellerId = :sellerId")
    long countSellerOrders(@Param("sellerId") Long sellerId);

    /**
     * 统计卖家指定状态的销售订单数量
     * @param sellerId 卖家ID
     * @param status 订单状态
     * @return 订单数量
     */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items oi WHERE oi.sellerId = :sellerId AND o.status = :status")
    long countSellerOrdersByStatus(@Param("sellerId") Long sellerId, @Param("status") Order.OrderStatus status);

    /**
     * 查询买家在指定时间范围内的订单
     * @param buyerId 买家ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单列表
     */
    List<Order> findByBuyerIdAndCreateTimeBetweenOrderByCreateTimeDesc(
            Long buyerId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询卖家在指定时间范围内的销售订单
     * @param sellerId 卖家ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单列表
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items oi WHERE oi.sellerId = :sellerId " +
           "AND o.createTime BETWEEN :startTime AND :endTime ORDER BY o.createTime DESC")
    List<Order> findSellerOrdersByTimeRange(@Param("sellerId") Long sellerId, 
                                           @Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 检查订单是否属于指定买家
     * @param orderId 订单ID
     * @param buyerId 买家ID
     * @return 是否存在
     */
    boolean existsByIdAndBuyerId(Long orderId, Long buyerId);

    /**
     * 检查订单是否包含指定卖家的商品
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items oi WHERE o.id = :orderId AND oi.sellerId = :sellerId")
    boolean existsByIdAndSellerId(@Param("orderId") Long orderId, @Param("sellerId") Long sellerId);

    /**
     * 查找超时的待支付订单
     * @param timeoutThreshold 超时时间阈值
     * @return 超时订单列表
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' AND o.createTime < :timeoutThreshold ORDER BY o.createTime ASC")
    List<Order> findTimeoutPendingOrders(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 查找指定时间范围内创建的待支付订单
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 待支付订单列表
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' AND o.createTime BETWEEN :startTime AND :endTime ORDER BY o.createTime ASC")
    List<Order> findPendingOrdersByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计超时的待支付订单数量
     * @param timeoutThreshold 超时时间阈值
     * @return 超时订单数量
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING_PAYMENT' AND o.createTime < :timeoutThreshold")
    long countTimeoutPendingOrders(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 查找最近的待支付订单（用于监控）
     * @param limit 限制数量
     * @return 最近的待支付订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' ORDER BY o.createTime DESC")
    List<Order> findRecentPendingOrders(org.springframework.data.domain.Pageable pageable);
}