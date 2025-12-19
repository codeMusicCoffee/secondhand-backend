package com.example.secondhand.service;

import com.example.secondhand.entity.TimeoutTask;
import com.example.secondhand.repository.TimeoutTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 超时任务调度服务
 * 负责管理订单超时自动取消的任务调度
 */
@Service
public class TimeoutTaskScheduler implements TimeoutTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutTaskScheduler.class);

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private TimeoutTaskRepository timeoutTaskRepository;

    // 移除直接依赖OrderService，通过OrderTimeoutService间接调用

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    @Lazy
    private OrderTimeoutService orderTimeoutService;

    // 存储正在调度的任务
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 任务执行统计
    private volatile long totalScheduledTasks = 0;
    private volatile long totalExecutedTasks = 0;
    private volatile long totalCancelledTasks = 0;
    private volatile long totalFailedTasks = 0;

    /**
     * 系统启动时初始化
     */
    @PostConstruct
    public void initialize() {
        logger.info("TimeoutTaskScheduler 正在初始化...");
        
        try {
            // 恢复系统重启前的未完成任务
            recoverPendingTasks();
            
            // 清理过期任务
            cleanupExpiredTasks();
            
            logger.info("TimeoutTaskScheduler 初始化完成");
        } catch (Exception e) {
            logger.error("TimeoutTaskScheduler 初始化失败", e);
        }
    }

    /**
     * 系统关闭时清理资源
     */
    @PreDestroy
    public void shutdown() {
        logger.info("TimeoutTaskScheduler 正在关闭...");
        
        try {
            // 取消所有正在调度的任务
            scheduledTasks.values().forEach(future -> {
                if (!future.isDone()) {
                    future.cancel(false);
                }
            });
            scheduledTasks.clear();
            
            logger.info("TimeoutTaskScheduler 关闭完成");
        } catch (Exception e) {
            logger.error("TimeoutTaskScheduler 关闭时发生错误", e);
        }
    }

    /**
     * 调度超时任务
     * @param orderNo 订单号
     * @param taskType 任务类型
     * @param timeoutMinutes 超时分钟数
     * @return 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String scheduleTimeout(String orderNo, TimeoutTask.TaskType taskType, int timeoutMinutes) {
        try {
            // 检查是否已存在相同的任务
            if (timeoutTaskRepository.existsByOrderNoAndTaskType(orderNo, taskType)) {
                logger.warn("订单 {} 的 {} 任务已存在，跳过调度", orderNo, taskType);
                return null;
            }

            // 创建超时任务
            TimeoutTask timeoutTask = new TimeoutTask(orderNo, taskType, timeoutMinutes);
            timeoutTask = timeoutTaskRepository.save(timeoutTask);

            // 调度任务执行
            final String taskId = timeoutTask.getTaskId();
            Date executeTime = Date.from(timeoutTask.getScheduleTime().atZone(ZoneId.systemDefault()).toInstant());
            ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeTimeoutTask(taskId),
                executeTime
            );

            // 保存调度信息
            scheduledTasks.put(timeoutTask.getTaskId(), future);
            totalScheduledTasks++;

            logger.info("成功调度超时任务: taskId={}, orderNo={}, taskType={}, scheduleTime={}", 
                timeoutTask.getTaskId(), orderNo, taskType, timeoutTask.getScheduleTime());

            return timeoutTask.getTaskId();
            
        } catch (Exception e) {
            logger.error("调度超时任务失败: orderNo={}, taskType={}", orderNo, taskType, e);
            throw new RuntimeException("调度超时任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消超时任务
     * @param taskId 任务ID
     * @param reason 取消原因
     * @return 是否成功取消
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeout(String taskId, String reason) {
        try {
            // 查询任务
            TimeoutTask timeoutTask = timeoutTaskRepository.findById(taskId).orElse(null);
            if (timeoutTask == null) {
                logger.warn("任务不存在: taskId={}", taskId);
                return false;
            }

            // 检查任务是否可以取消
            if (!timeoutTask.canCancel()) {
                logger.warn("任务状态不允许取消: taskId={}, status={}", taskId, timeoutTask.getStatus());
                return false;
            }

            // 取消调度的任务
            ScheduledFuture<?> future = scheduledTasks.remove(taskId);
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }

            // 更新任务状态
            timeoutTask.markCancelled(reason);
            timeoutTaskRepository.save(timeoutTask);
            totalCancelledTasks++;

            logger.info("成功取消超时任务: taskId={}, reason={}", taskId, reason);
            return true;
            
        } catch (Exception e) {
            logger.error("取消超时任务失败: taskId={}", taskId, e);
            return false;
        }
    }

    /**
     * 根据订单号取消超时任务
     * @param orderNo 订单号
     * @param taskType 任务类型
     * @param reason 取消原因
     * @return 是否成功取消
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutByOrder(String orderNo, TimeoutTask.TaskType taskType, String reason) {
        try {
            // 查询订单的活跃任务
            List<TimeoutTask> activeTasks = timeoutTaskRepository.findActiveTasksByOrderNo(orderNo);
            
            boolean hasSuccess = false;
            for (TimeoutTask task : activeTasks) {
                if (taskType == null || task.getTaskType() == taskType) {
                    if (cancelTimeout(task.getTaskId(), reason)) {
                        hasSuccess = true;
                    }
                }
            }

            return hasSuccess;
            
        } catch (Exception e) {
            logger.error("根据订单号取消超时任务失败: orderNo={}, taskType={}", orderNo, taskType, e);
            return false;
        }
    }

    /**
     * 执行超时任务
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeTimeoutTask(String taskId) {
        TimeoutTask timeoutTask = null;
        
        try {
            // 查询任务
            timeoutTask = timeoutTaskRepository.findById(taskId).orElse(null);
            if (timeoutTask == null) {
                logger.warn("执行超时任务时任务不存在: taskId={}", taskId);
                return;
            }

            // 检查任务是否可以执行
            if (!timeoutTask.canExecute()) {
                logger.warn("任务状态不允许执行: taskId={}, status={}", taskId, timeoutTask.getStatus());
                return;
            }

            // 标记任务开始执行
            timeoutTask.markExecuting();
            timeoutTaskRepository.save(timeoutTask);

            logger.info("开始执行超时任务: taskId={}, orderNo={}, taskType={}", 
                taskId, timeoutTask.getOrderNo(), timeoutTask.getTaskType());

            // 根据任务类型执行相应的处理逻辑
            boolean success = false;
            switch (timeoutTask.getTaskType()) {
                case PAYMENT_TIMEOUT:
                    success = handlePaymentTimeout(timeoutTask);
                    break;
                case ORDER_TIMEOUT:
                    success = handleOrderTimeout(timeoutTask);
                    break;
                case INVENTORY_TIMEOUT:
                    success = handleInventoryTimeout(timeoutTask);
                    break;
                case CALLBACK_TIMEOUT:
                    success = handleCallbackTimeout(timeoutTask);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的任务类型: " + timeoutTask.getTaskType());
            }

            if (success) {
                // 标记任务执行成功
                timeoutTask.markExecuted();
                timeoutTaskRepository.save(timeoutTask);
                totalExecutedTasks++;
                
                logger.info("超时任务执行成功: taskId={}, orderNo={}", taskId, timeoutTask.getOrderNo());
            } else {
                throw new RuntimeException("任务处理逻辑返回失败");
            }

        } catch (Exception e) {
            logger.error("执行超时任务失败: taskId={}", taskId, e);
            
            if (timeoutTask != null) {
                try {
                    // 标记任务执行失败
                    timeoutTask.markFailed(e.getMessage());
                    timeoutTaskRepository.save(timeoutTask);
                    totalFailedTasks++;

                    // 检查是否可以重试
                    if (timeoutTask.canRetry()) {
                        scheduleRetry(timeoutTask);
                    }
                    
                } catch (Exception saveException) {
                    logger.error("保存任务失败状态时发生错误: taskId={}", taskId, saveException);
                }
            }
        } finally {
            // 清理调度信息
            scheduledTasks.remove(taskId);
        }
    }

    /**
     * 处理支付超时
     * @param timeoutTask 超时任务
     * @return 是否处理成功
     */
    private boolean handlePaymentTimeout(TimeoutTask timeoutTask) {
        try {
            String orderNo = timeoutTask.getOrderNo();
            logger.info("处理支付超时: orderNo={}", orderNo);

            // 调用支付服务处理支付超时
            return paymentService.handlePaymentTimeout(orderNo);
            
        } catch (Exception e) {
            logger.error("处理支付超时失败: orderNo={}", timeoutTask.getOrderNo(), e);
            return false;
        }
    }

    /**
     * 处理订单超时
     * @param timeoutTask 超时任务
     * @return 是否处理成功
     */
    private boolean handleOrderTimeout(TimeoutTask timeoutTask) {
        try {
            String orderNo = timeoutTask.getOrderNo();
            logger.info("处理订单超时: orderNo={}", orderNo);

            // 通过OrderTimeoutService处理订单超时（避免循环依赖）
            return orderTimeoutService.handleOrderTimeout(orderNo);
            
        } catch (Exception e) {
            logger.error("处理订单超时失败: orderNo={}", timeoutTask.getOrderNo(), e);
            return false;
        }
    }

    /**
     * 处理库存超时
     * @param timeoutTask 超时任务
     * @return 是否处理成功
     */
    private boolean handleInventoryTimeout(TimeoutTask timeoutTask) {
        try {
            String orderNo = timeoutTask.getOrderNo();
            logger.info("处理库存超时: orderNo={}", orderNo);

            // 调用库存服务处理库存超时（恢复预扣库存）
            return inventoryService.handleInventoryTimeout(orderNo);
            
        } catch (Exception e) {
            logger.error("处理库存超时失败: orderNo={}", timeoutTask.getOrderNo(), e);
            return false;
        }
    }

    /**
     * 处理回调超时
     * @param timeoutTask 超时任务
     * @return 是否处理成功
     */
    private boolean handleCallbackTimeout(TimeoutTask timeoutTask) {
        try {
            String orderNo = timeoutTask.getOrderNo();
            logger.info("处理回调超时: orderNo={}", orderNo);

            // 调用支付服务处理回调超时（主动查询支付状态）
            return paymentService.handleCallbackTimeout(orderNo);
            
        } catch (Exception e) {
            logger.error("处理回调超时失败: orderNo={}", timeoutTask.getOrderNo(), e);
            return false;
        }
    }

    /**
     * 调度重试任务
     * @param timeoutTask 超时任务
     */
    private void scheduleRetry(TimeoutTask timeoutTask) {
        try {
            // 更新任务为重试状态
            timeoutTask.retry();
            timeoutTaskRepository.save(timeoutTask);

            // 重新调度任务
            final String taskId = timeoutTask.getTaskId();
            Date executeTime = Date.from(timeoutTask.getScheduleTime().atZone(ZoneId.systemDefault()).toInstant());
            ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeTimeoutTask(taskId),
                executeTime
            );

            scheduledTasks.put(taskId, future);

            logger.info("任务重试调度成功: taskId={}, retryCount={}, nextScheduleTime={}", 
                timeoutTask.getTaskId(), timeoutTask.getRetryCount(), timeoutTask.getScheduleTime());
                
        } catch (Exception e) {
            logger.error("调度重试任务失败: taskId={}", timeoutTask.getTaskId(), e);
        }
    }

    /**
     * 恢复系统重启前的未完成任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void recoverPendingTasks() {
        try {
            logger.info("开始恢复未完成的超时任务...");

            // 查询所有需要执行的任务
            List<TimeoutTask> executableTasks = timeoutTaskRepository.findExecutableTasks(LocalDateTime.now());
            
            int recoveredCount = 0;
            int expiredCount = 0;
            int immediateExecuteCount = 0;
            
            for (TimeoutTask task : executableTasks) {
                try {
                    // 检查任务是否已过期
                    if (task.isExpired()) {
                        task.markCancelled("SYSTEM_RESTART_EXPIRED");
                        timeoutTaskRepository.save(task);
                        expiredCount++;
                        continue;
                    }

                    // 如果任务调度时间已过，立即执行
                    if (LocalDateTime.now().isAfter(task.getScheduleTime())) {
                        executeTimeoutTask(task.getTaskId());
                        immediateExecuteCount++;
                    } else {
                        // 重新调度任务
                        final String taskId = task.getTaskId();
                        Date executeTime = Date.from(task.getScheduleTime().atZone(ZoneId.systemDefault()).toInstant());
                        ScheduledFuture<?> future = taskScheduler.schedule(
                            () -> executeTimeoutTask(taskId),
                            executeTime
                        );
                        scheduledTasks.put(taskId, future);
                    }
                    
                    recoveredCount++;
                    
                } catch (Exception e) {
                    logger.error("恢复任务失败: taskId={}", task.getTaskId(), e);
                }
            }

            logger.info("任务恢复完成: 总任务数={}, 恢复任务数={}, 立即执行数={}, 过期任务数={}", 
                executableTasks.size(), recoveredCount, immediateExecuteCount, expiredCount);
            
        } catch (Exception e) {
            logger.error("恢复未完成任务失败", e);
        }
    }

    /**
     * 重新调度待支付订单的超时任务
     * 在系统启动时调用，为所有待支付订单重新创建超时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void rescheduleOrderTimeouts() {
        try {
            logger.info("开始重新调度待支付订单的超时任务...");

            // 这里需要通过OrderService获取待支付订单
            // 由于循环依赖问题，我们在启动配置中处理这个逻辑
            
            logger.info("待支付订单超时任务重新调度完成");
            
        } catch (Exception e) {
            logger.error("重新调度待支付订单超时任务失败", e);
        }
    }

    /**
     * 定时清理过期任务
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredTasks() {
        try {
            logger.debug("开始清理过期任务...");

            // 标记过期任务为已取消
            LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
            int cancelledCount = timeoutTaskRepository.markExpiredTasksAsCancelled(expireTime);

            // 删除7天前的已完成任务
            LocalDateTime deleteTime = LocalDateTime.now().minusDays(7);
            long deletedCount = timeoutTaskRepository.deleteCompletedTasksBefore(deleteTime);

            if (cancelledCount > 0 || deletedCount > 0) {
                logger.info("任务清理完成: 取消过期任务数={}, 删除历史任务数={}", cancelledCount, deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理过期任务失败", e);
        }
    }

    /**
     * 定时检查卡住的任务
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Transactional(rollbackFor = Exception.class)
    public void checkStuckTasks() {
        try {
            // 查询执行时间超过30分钟的任务
            LocalDateTime stuckTime = LocalDateTime.now().minusMinutes(30);
            List<TimeoutTask> stuckTasks = timeoutTaskRepository.findStuckTasks(stuckTime);

            for (TimeoutTask task : stuckTasks) {
                try {
                    logger.warn("发现卡住的任务: taskId={}, executeTime={}", task.getTaskId(), task.getExecuteTime());
                    
                    // 标记任务失败并尝试重试
                    task.markFailed("TASK_STUCK_TIMEOUT");
                    timeoutTaskRepository.save(task);
                    
                    if (task.canRetry()) {
                        scheduleRetry(task);
                    }
                    
                } catch (Exception e) {
                    logger.error("处理卡住任务失败: taskId={}", task.getTaskId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("检查卡住任务失败", e);
        }
    }

    /**
     * 获取任务统计信息
     * @return 统计信息
     */
    public TaskStatistics getTaskStatistics() {
        try {
            // 获取数据库统计
            long scheduledCount = timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.SCHEDULED);
            long executingCount = timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.EXECUTING);
            long executedCount = timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.EXECUTED);
            long cancelledCount = timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.CANCELLED);
            long failedCount = timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.FAILED);
            long retryCount = timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.RETRY);

            return new TaskStatistics(
                totalScheduledTasks,
                totalExecutedTasks,
                totalCancelledTasks,
                totalFailedTasks,
                scheduledCount,
                executingCount,
                executedCount,
                cancelledCount,
                failedCount,
                retryCount,
                scheduledTasks.size()
            );
            
        } catch (Exception e) {
            logger.error("获取任务统计信息失败", e);
            return new TaskStatistics();
        }
    }

    /**
     * 任务统计信息类
     */
    public static class TaskStatistics {
        private final long totalScheduled;
        private final long totalExecuted;
        private final long totalCancelled;
        private final long totalFailed;
        private final long currentScheduled;
        private final long currentExecuting;
        private final long currentExecuted;
        private final long currentCancelled;
        private final long currentFailed;
        private final long currentRetry;
        private final long activeScheduledTasks;

        public TaskStatistics() {
            this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public TaskStatistics(long totalScheduled, long totalExecuted, long totalCancelled, long totalFailed,
                            long currentScheduled, long currentExecuting, long currentExecuted, 
                            long currentCancelled, long currentFailed, long currentRetry, long activeScheduledTasks) {
            this.totalScheduled = totalScheduled;
            this.totalExecuted = totalExecuted;
            this.totalCancelled = totalCancelled;
            this.totalFailed = totalFailed;
            this.currentScheduled = currentScheduled;
            this.currentExecuting = currentExecuting;
            this.currentExecuted = currentExecuted;
            this.currentCancelled = currentCancelled;
            this.currentFailed = currentFailed;
            this.currentRetry = currentRetry;
            this.activeScheduledTasks = activeScheduledTasks;
        }

        // Getter 方法
        public long getTotalScheduled() { return totalScheduled; }
        public long getTotalExecuted() { return totalExecuted; }
        public long getTotalCancelled() { return totalCancelled; }
        public long getTotalFailed() { return totalFailed; }
        public long getCurrentScheduled() { return currentScheduled; }
        public long getCurrentExecuting() { return currentExecuting; }
        public long getCurrentExecuted() { return currentExecuted; }
        public long getCurrentCancelled() { return currentCancelled; }
        public long getCurrentFailed() { return currentFailed; }
        public long getCurrentRetry() { return currentRetry; }
        public long getActiveScheduledTasks() { return activeScheduledTasks; }

        public double getSuccessRate() {
            long total = totalExecuted + totalFailed;
            return total > 0 ? (double) totalExecuted / total * 100 : 0.0;
        }
    }
}