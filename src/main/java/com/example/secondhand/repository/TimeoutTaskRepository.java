package com.example.secondhand.repository;

import com.example.secondhand.entity.TimeoutTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 超时任务数据访问接口
 */
@Repository
public interface TimeoutTaskRepository extends JpaRepository<TimeoutTask, String> {

    /**
     * 根据订单号查询超时任务
     * @param orderNo 订单号
     * @return 超时任务列表
     */
    List<TimeoutTask> findByOrderNoOrderByCreateTimeDesc(String orderNo);

    /**
     * 根据订单号和任务类型查询超时任务
     * @param orderNo 订单号
     * @param taskType 任务类型
     * @return 超时任务
     */
    Optional<TimeoutTask> findByOrderNoAndTaskType(String orderNo, TimeoutTask.TaskType taskType);

    /**
     * 根据任务状态查询超时任务列表
     * @param status 任务状态
     * @return 超时任务列表
     */
    List<TimeoutTask> findByStatusOrderByScheduleTimeAsc(TimeoutTask.TaskStatus status);

    /**
     * 根据任务类型查询超时任务列表
     * @param taskType 任务类型
     * @return 超时任务列表
     */
    List<TimeoutTask> findByTaskTypeOrderByScheduleTimeAsc(TimeoutTask.TaskType taskType);

    /**
     * 根据任务状态和类型查询超时任务列表
     * @param status 任务状态
     * @param taskType 任务类型
     * @return 超时任务列表
     */
    List<TimeoutTask> findByStatusAndTaskTypeOrderByScheduleTimeAsc(
            TimeoutTask.TaskStatus status, TimeoutTask.TaskType taskType);

    /**
     * 查询需要执行的任务（已调度且调度时间已到）
     * @param currentTime 当前时间
     * @return 需要执行的任务列表
     */
    @Query("SELECT t FROM TimeoutTask t WHERE t.status IN ('SCHEDULED', 'RETRY') AND t.scheduleTime <= :currentTime ORDER BY t.scheduleTime ASC")
    List<TimeoutTask> findExecutableTasks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 查询指定时间范围内创建的超时任务
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 超时任务列表
     */
    List<TimeoutTask> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内需要调度的任务
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 超时任务列表
     */
    List<TimeoutTask> findByScheduleTimeBetweenOrderByScheduleTimeAsc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内指定状态的任务
     * @param status 任务状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 超时任务列表
     */
    List<TimeoutTask> findByStatusAndScheduleTimeBetweenOrderByScheduleTimeAsc(
            TimeoutTask.TaskStatus status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询过期的任务（创建时间超过24小时且未完成）
     * @param expireTime 过期时间点
     * @return 过期任务列表
     */
    @Query("SELECT t FROM TimeoutTask t WHERE t.status IN ('SCHEDULED', 'RETRY', 'FAILED') AND t.createTime < :expireTime")
    List<TimeoutTask> findExpiredTasks(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 查询失败且可以重试的任务
     * @return 可重试的任务列表
     */
    @Query("SELECT t FROM TimeoutTask t WHERE t.status = 'FAILED' AND t.retryCount < t.maxRetryCount")
    List<TimeoutTask> findRetryableTasks();

    /**
     * 查询执行中的任务（可能需要检查是否卡住）
     * @param executingTime 执行时间上限
     * @return 执行中的任务列表
     */
    @Query("SELECT t FROM TimeoutTask t WHERE t.status = 'EXECUTING' AND t.executeTime < :executingTime")
    List<TimeoutTask> findStuckTasks(@Param("executingTime") LocalDateTime executingTime);

    /**
     * 统计任务状态分布
     * @return 统计结果 [状态, 数量]
     */
    @Query("SELECT t.status, COUNT(t) FROM TimeoutTask t GROUP BY t.status")
    List<Object[]> getTaskStatusStatistics();

    /**
     * 统计任务类型分布
     * @return 统计结果 [类型, 数量]
     */
    @Query("SELECT t.taskType, COUNT(t) FROM TimeoutTask t GROUP BY t.taskType")
    List<Object[]> getTaskTypeStatistics();

    /**
     * 统计指定状态的任务数量
     * @param status 任务状态
     * @return 任务数量
     */
    long countByStatus(TimeoutTask.TaskStatus status);

    /**
     * 统计指定类型的任务数量
     * @param taskType 任务类型
     * @return 任务数量
     */
    long countByTaskType(TimeoutTask.TaskType taskType);

    /**
     * 统计指定时间范围内的任务数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务数量
     */
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定时间范围内指定状态的任务数量
     * @param status 任务状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务数量
     */
    long countByStatusAndCreateTimeBetween(TimeoutTask.TaskStatus status, 
                                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 检查订单是否已有指定类型的任务
     * @param orderNo 订单号
     * @param taskType 任务类型
     * @return 是否存在
     */
    boolean existsByOrderNoAndTaskType(String orderNo, TimeoutTask.TaskType taskType);

    /**
     * 检查订单是否有未完成的任务
     * @param orderNo 订单号
     * @return 是否存在
     */
    @Query("SELECT COUNT(t) > 0 FROM TimeoutTask t WHERE t.orderNo = :orderNo AND t.status IN ('SCHEDULED', 'EXECUTING', 'RETRY')")
    boolean existsActiveTaskByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询订单的活跃任务（未完成的任务）
     * @param orderNo 订单号
     * @return 活跃任务列表
     */
    @Query("SELECT t FROM TimeoutTask t WHERE t.orderNo = :orderNo AND t.status IN ('SCHEDULED', 'EXECUTING', 'RETRY') ORDER BY t.createTime DESC")
    List<TimeoutTask> findActiveTasksByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询最近的任务记录（用于监控）
     * @param pageable 分页参数
     * @return 任务列表
     */
    @Query("SELECT t FROM TimeoutTask t ORDER BY t.createTime DESC")
    List<TimeoutTask> findRecentTasks(org.springframework.data.domain.Pageable pageable);

    /**
     * 查询最近执行的任务
     * @param pageable 分页参数
     * @return 任务列表
     */
    @Query("SELECT t FROM TimeoutTask t WHERE t.executeTime IS NOT NULL ORDER BY t.executeTime DESC")
    List<TimeoutTask> findRecentExecutedTasks(org.springframework.data.domain.Pageable pageable);

    /**
     * 查询任务执行成功率
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果 [总数, 成功数]
     */
    @Query("SELECT COUNT(t), SUM(CASE WHEN t.status = 'EXECUTED' THEN 1 ELSE 0 END) " +
           "FROM TimeoutTask t WHERE t.createTime BETWEEN :startTime AND :endTime")
    Object[] getTaskSuccessRate(@Param("startTime") LocalDateTime startTime, 
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均任务执行时长（分钟）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均执行时长
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, t.scheduleTime, t.executeTime)) " +
           "FROM TimeoutTask t WHERE t.status = 'EXECUTED' AND t.executeTime IS NOT NULL " +
           "AND t.createTime BETWEEN :startTime AND :endTime")
    Double getAverageExecutionDuration(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的已完成任务（用于数据清理）
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM TimeoutTask t WHERE t.status IN ('EXECUTED', 'CANCELLED') AND t.updateTime < :beforeTime")
    long deleteCompletedTasksBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 批量更新过期任务状态
     * @param expireTime 过期时间点
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE TimeoutTask t SET t.status = 'CANCELLED', t.cancelReason = 'EXPIRED', t.updateTime = CURRENT_TIMESTAMP " +
           "WHERE t.status IN ('SCHEDULED', 'RETRY') AND t.createTime < :expireTime")
    int markExpiredTasksAsCancelled(@Param("expireTime") LocalDateTime expireTime);
}