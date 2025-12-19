package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 超时任务实体类
 * 管理订单超时自动取消的任务调度
 */
@Entity
@Table(name = "timeout_tasks", indexes = {
    @Index(name = "idx_order_no", columnList = "order_no"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_schedule_time", columnList = "schedule_time"),
    @Index(name = "idx_task_type", columnList = "task_type")
})
public class TimeoutTask {

    @Id
    @Column(name = "task_id", length = 100)
    private String taskId;            // 任务ID（主键）

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;           // 订单号

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;        // 任务状态

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;        // 任务类型

    @Column(name = "schedule_time", nullable = false)
    private LocalDateTime scheduleTime; // 调度时间

    @Column(name = "execute_time")
    private LocalDateTime executeTime;  // 执行时间

    @Column(name = "timeout_minutes", nullable = false)
    private Integer timeoutMinutes;   // 超时分钟数

    @Column(name = "retry_count")
    private Integer retryCount;       // 重试次数

    @Column(name = "max_retry_count")
    private Integer maxRetryCount;    // 最大重试次数

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;      // 错误信息

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime; // 创建时间

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;      // 取消原因

    // 任务状态枚举
    public enum TaskStatus {
        SCHEDULED("已调度"),
        EXECUTING("执行中"),
        EXECUTED("已执行"),
        CANCELLED("已取消"),
        FAILED("执行失败"),
        RETRY("重试中");

        private final String description;

        TaskStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 任务类型枚举
    public enum TaskType {
        PAYMENT_TIMEOUT("支付超时"),
        ORDER_TIMEOUT("订单超时"),
        INVENTORY_TIMEOUT("库存超时"),
        CALLBACK_TIMEOUT("回调超时");

        private final String description;

        TaskType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 构造函数
    public TimeoutTask() {}

    public TimeoutTask(String orderNo, TaskType taskType, int timeoutMinutes) {
        this.taskId = generateTaskId(orderNo, taskType);
        this.orderNo = orderNo;
        this.taskType = taskType;
        this.timeoutMinutes = timeoutMinutes;
        this.status = TaskStatus.SCHEDULED;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.scheduleTime = LocalDateTime.now().plusMinutes(timeoutMinutes);
    }

    // Getter 和 Setter 方法
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public LocalDateTime getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(LocalDateTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public LocalDateTime getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(LocalDateTime executeTime) {
        this.executeTime = executeTime;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    // 业务方法
    /**
     * 生成任务ID
     */
    public static String generateTaskId(String orderNo, TaskType taskType) {
        return String.format("%s_%s_%d", orderNo, taskType.name(), System.currentTimeMillis());
    }

    /**
     * 检查任务是否可以执行
     */
    public boolean canExecute() {
        return this.status == TaskStatus.SCHEDULED && 
               LocalDateTime.now().isAfter(this.scheduleTime);
    }

    /**
     * 检查任务是否可以取消
     */
    public boolean canCancel() {
        return this.status == TaskStatus.SCHEDULED || this.status == TaskStatus.RETRY;
    }

    /**
     * 检查任务是否可以重试
     */
    public boolean canRetry() {
        return this.status == TaskStatus.FAILED && 
               this.retryCount < this.maxRetryCount;
    }

    /**
     * 标记任务开始执行
     */
    public void markExecuting() {
        if (this.status != TaskStatus.SCHEDULED && this.status != TaskStatus.RETRY) {
            throw new IllegalStateException(
                String.format("Cannot execute task in status: %s", this.status));
        }
        this.status = TaskStatus.EXECUTING;
        this.executeTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记任务执行成功
     */
    public void markExecuted() {
        if (this.status != TaskStatus.EXECUTING) {
            throw new IllegalStateException(
                String.format("Cannot mark executed for task in status: %s", this.status));
        }
        this.status = TaskStatus.EXECUTED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记任务执行失败
     */
    public void markFailed(String errorMessage) {
        if (this.status != TaskStatus.EXECUTING) {
            throw new IllegalStateException(
                String.format("Cannot mark failed for task in status: %s", this.status));
        }
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记任务取消
     */
    public void markCancelled(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException(
                String.format("Cannot cancel task in status: %s", this.status));
        }
        this.status = TaskStatus.CANCELLED;
        this.cancelReason = reason;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 重试任务
     */
    public void retry() {
        if (!canRetry()) {
            throw new IllegalStateException(
                String.format("Cannot retry task: status=%s, retryCount=%d, maxRetryCount=%d", 
                    this.status, this.retryCount, this.maxRetryCount));
        }
        
        this.retryCount++;
        this.status = TaskStatus.RETRY;
        this.errorMessage = null;
        
        // 延迟重试时间（指数退避）
        int delayMinutes = (int) Math.pow(2, this.retryCount);
        this.scheduleTime = LocalDateTime.now().plusMinutes(delayMinutes);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 检查任务是否已完成（成功或取消）
     */
    public boolean isCompleted() {
        return this.status == TaskStatus.EXECUTED || 
               this.status == TaskStatus.CANCELLED;
    }

    /**
     * 检查任务是否已过期
     */
    public boolean isExpired() {
        if (this.status != TaskStatus.SCHEDULED && this.status != TaskStatus.RETRY) {
            return false;
        }
        
        // 任务创建后24小时过期
        LocalDateTime expireTime = this.createTime.plusHours(24);
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 获取任务描述
     */
    public String getDescription() {
        return String.format("%s - 订单: %s, 超时: %d分钟", 
            this.taskType.getDescription(), 
            this.orderNo, 
            this.timeoutMinutes);
    }

    /**
     * 获取剩余时间（分钟）
     */
    public long getRemainingMinutes() {
        if (this.status != TaskStatus.SCHEDULED && this.status != TaskStatus.RETRY) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(this.scheduleTime)) {
            return 0;
        }
        
        return java.time.Duration.between(now, this.scheduleTime).toMinutes();
    }

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        if (this.updateTime == null) {
            this.updateTime = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = TaskStatus.SCHEDULED;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        if (this.maxRetryCount == null) {
            this.maxRetryCount = 3;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}