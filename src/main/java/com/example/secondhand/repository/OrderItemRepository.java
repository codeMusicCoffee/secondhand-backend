package com.example.secondhand.repository;

import com.example.secondhand.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 根据订单ID查询订单项列表
     * @param orderId 订单ID
     * @return 订单项列表
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 根据订单号查询订单项列表
     * @param orderNo 订单号
     * @return 订单项列表
     */
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.orderNo = :orderNo")
    List<OrderItem> findByOrderOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据商品ID查询订单项列表
     * @param productId 商品ID
     * @return 订单项列表
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * 根据卖家ID查询订单项列表
     * @param sellerId 卖家ID
     * @return 订单项列表
     */
    List<OrderItem> findBySellerIdOrderByOrderCreateTimeDesc(Long sellerId);

    /**
     * 根据订单ID和卖家ID查询订单项列表
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return 订单项列表
     */
    List<OrderItem> findByOrderIdAndSellerId(Long orderId, Long sellerId);

    /**
     * 统计商品的销售数量
     * @param productId 商品ID
     * @return 销售数量
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    int sumQuantityByProductId(@Param("productId") Long productId);

    /**
     * 统计卖家的销售数量
     * @param sellerId 卖家ID
     * @return 销售数量
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.sellerId = :sellerId")
    int sumQuantityBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 统计卖家的销售金额
     * @param sellerId 卖家ID
     * @return 销售金额
     */
    @Query("SELECT COALESCE(SUM(oi.subtotal), 0) FROM OrderItem oi WHERE oi.sellerId = :sellerId")
    BigDecimal sumAmountBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 统计卖家在指定时间范围内的销售金额
     * @param sellerId 卖家ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 销售金额
     */
    @Query("SELECT COALESCE(SUM(oi.subtotal), 0) FROM OrderItem oi JOIN oi.order o " +
           "WHERE oi.sellerId = :sellerId AND o.createTime BETWEEN :startTime AND :endTime")
    BigDecimal sumAmountBySellerIdAndTimeRange(@Param("sellerId") Long sellerId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计卖家在指定时间范围内的销售数量
     * @param sellerId 卖家ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 销售数量
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o " +
           "WHERE oi.sellerId = :sellerId AND o.createTime BETWEEN :startTime AND :endTime")
    int sumQuantityBySellerIdAndTimeRange(@Param("sellerId") Long sellerId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询卖家的热销商品（按销售数量排序）
     * @param sellerId 卖家ID
     * @param limit 限制数量
     * @return 商品销售统计列表
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQuantity, SUM(oi.subtotal) as totalAmount " +
           "FROM OrderItem oi WHERE oi.sellerId = :sellerId " +
           "GROUP BY oi.productId, oi.productName " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProductsBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 查询商品的购买用户列表
     * @param productId 商品ID
     * @return 订单项列表
     */
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE oi.productId = :productId ORDER BY o.createTime DESC")
    List<OrderItem> findBuyersByProductId(@Param("productId") Long productId);

    /**
     * 统计订单项数量
     * @param orderId 订单ID
     * @return 订单项数量
     */
    long countByOrderId(Long orderId);

    /**
     * 检查订单项是否存在
     * @param orderId 订单ID
     * @param productId 商品ID
     * @return 是否存在
     */
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);

    /**
     * 查询卖家在指定订单中的订单项
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return 订单项列表
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.sellerId = :sellerId")
    List<OrderItem> findSellerItemsByOrderId(@Param("orderId") Long orderId, @Param("sellerId") Long sellerId);

    /**
     * 统计卖家的订单项数量
     * @param sellerId 卖家ID
     * @return 订单项数量
     */
    long countBySellerId(Long sellerId);
}