package com.example.secondhand.repository;

import com.example.secondhand.entity.PaymentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付日志数据访问接口
 */
@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    /**
     * 根据订单号查询支付日志列表
     * @param orderNo 订单号
     * @return 支付日志列表
     */
    List<PaymentLog> findByOrderNoOrderByCreateTimeDesc(String orderNo);

    /**
     * 根据订单号分页查询支付日志
     * @param orderNo 订单号
     * @param pageable 分页参数
     * @return 支付日志分页结果
     */
    Page<PaymentLog> findByOrderNoOrderByCreateTimeDesc(String orderNo, Pageable pageable);

    /**
     * 根据支付宝交易号查询支付日志列表
     * @param alipayTradeNo 支付宝交易号
     * @return 支付日志列表
     */
    List<PaymentLog> findByAlipayTradeNoOrderByCreateTimeDesc(String alipayTradeNo);

    /**
     * 根据操作类型查询支付日志列表
     * @param operation 操作类型
     * @return 支付日志列表
     */
    List<PaymentLog> findByOperationOrderByCreateTimeDesc(PaymentLog.LogOperation operation);

    /**
     * 根据日志级别查询支付日志列表
     * @param level 日志级别
     * @return 支付日志列表
     */
    List<PaymentLog> findByLevelOrderByCreateTimeDesc(PaymentLog.LogLevel level);

    /**
     * 根据用户ID查询支付日志列表
     * @param userId 用户ID
     * @return 支付日志列表
     */
    List<PaymentLog> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 查询指定时间范围内的支付日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付日志列表
     */
    List<PaymentLog> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询指定时间范围内的支付日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 支付日志分页结果
     */
    Page<PaymentLog> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询指定时间范围内指定级别的支付日志
     * @param level 日志级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付日志列表
     */
    List<PaymentLog> findByLevelAndCreateTimeBetweenOrderByCreateTimeDesc(
            PaymentLog.LogLevel level, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内指定操作类型的支付日志
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付日志列表
     */
    List<PaymentLog> findByOperationAndCreateTimeBetweenOrderByCreateTimeDesc(
            PaymentLog.LogOperation operation, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询订单在指定时间范围内的支付日志
     * @param orderNo 订单号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付日志列表
     */
    List<PaymentLog> findByOrderNoAndCreateTimeBetweenOrderByCreateTimeDesc(
            String orderNo, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询错误日志（ERROR级别）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误日志列表
     */
    @Query("SELECT p FROM PaymentLog p WHERE p.level = 'ERROR' AND p.createTime BETWEEN :startTime AND :endTime ORDER BY p.createTime DESC")
    List<PaymentLog> findErrorLogs(@Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询警告日志（WARN级别）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 警告日志列表
     */
    @Query("SELECT p FROM PaymentLog p WHERE p.level = 'WARN' AND p.createTime BETWEEN :startTime AND :endTime ORDER BY p.createTime DESC")
    List<PaymentLog> findWarnLogs(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计订单的日志数量
     * @param orderNo 订单号
     * @return 日志数量
     */
    long countByOrderNo(String orderNo);

    /**
     * 统计指定级别的日志数量
     * @param level 日志级别
     * @return 日志数量
     */
    long countByLevel(PaymentLog.LogLevel level);

    /**
     * 统计指定操作类型的日志数量
     * @param operation 操作类型
     * @return 日志数量
     */
    long countByOperation(PaymentLog.LogOperation operation);

    /**
     * 统计指定时间范围内的日志数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定时间范围内指定级别的日志数量
     * @param level 日志级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    long countByLevelAndCreateTimeBetween(PaymentLog.LogLevel level, 
                                         LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定时间范围内指定操作类型的日志数量
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    long countByOperationAndCreateTimeBetween(PaymentLog.LogOperation operation, 
                                             LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近的日志记录（用于监控）
     * @param pageable 分页参数
     * @return 日志列表
     */
    @Query("SELECT p FROM PaymentLog p ORDER BY p.createTime DESC")
    List<PaymentLog> findRecentLogs(org.springframework.data.domain.Pageable pageable);

    /**
     * 查询最近的错误日志（用于监控）
     * @param pageable 分页参数
     * @return 错误日志列表
     */
    @Query("SELECT p FROM PaymentLog p WHERE p.level = 'ERROR' ORDER BY p.createTime DESC")
    List<PaymentLog> findRecentErrorLogs(org.springframework.data.domain.Pageable pageable);

    /**
     * 查询操作统计信息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果 [操作类型, 数量]
     */
    @Query("SELECT p.operation, COUNT(p) FROM PaymentLog p WHERE p.createTime BETWEEN :startTime AND :endTime GROUP BY p.operation ORDER BY COUNT(p) DESC")
    List<Object[]> getOperationStatistics(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询错误统计信息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果 [日志级别, 数量]
     */
    @Query("SELECT p.level, COUNT(p) FROM PaymentLog p WHERE p.createTime BETWEEN :startTime AND :endTime GROUP BY p.level ORDER BY COUNT(p) DESC")
    List<Object[]> getErrorStatistics(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均执行时间
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均执行时间（毫秒）
     */
    @Query("SELECT AVG(p.executionTime) FROM PaymentLog p WHERE p.operation = :operation " +
           "AND p.executionTime IS NOT NULL AND p.createTime BETWEEN :startTime AND :endTime")
    Double getAverageExecutionTime(@Param("operation") PaymentLog.LogOperation operation,
                                  @Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的日志（用于日志清理）
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    long deleteByCreateTimeBefore(LocalDateTime beforeTime);

    /**
     * 复合查询：根据多个条件查询日志
     * @param orderNo 订单号（可选）
     * @param operation 操作类型（可选）
     * @param level 日志级别（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    @Query("SELECT p FROM PaymentLog p WHERE " +
           "(:orderNo IS NULL OR p.orderNo = :orderNo) AND " +
           "(:operation IS NULL OR p.operation = :operation) AND " +
           "(:level IS NULL OR p.level = :level) AND " +
           "p.createTime BETWEEN :startTime AND :endTime " +
           "ORDER BY p.createTime DESC")
    Page<PaymentLog> findByConditions(@Param("orderNo") String orderNo,
                                     @Param("operation") PaymentLog.LogOperation operation,
                                     @Param("level") PaymentLog.LogLevel level,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     Pageable pageable);
}