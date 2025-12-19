package com.example.secondhand.service;

import com.example.secondhand.entity.TimeoutTask;

/**
 * 超时任务管理接口
 * 用于解耦TimeoutTaskScheduler和OrderTimeoutService之间的循环依赖
 */
public interface TimeoutTaskManager {
    
    /**
     * 调度超时任务
     * @param orderNo 订单号
     * @param taskType 任务类型
     * @param timeoutMinutes 超时分钟数
     * @return 任务ID
     */
    String scheduleTimeout(String orderNo, TimeoutTask.TaskType taskType, int timeoutMinutes);
    
    /**
     * 取消超时任务
     * @param taskId 任务ID
     * @param reason 取消原因
     * @return 是否成功取消
     */
    boolean cancelTimeout(String taskId, String reason);
    
    /**
     * 根据订单号取消超时任务
     * @param orderNo 订单号
     * @param taskType 任务类型
     * @param reason 取消原因
     * @return 是否成功取消
     */
    boolean cancelTimeoutByOrder(String orderNo, TimeoutTask.TaskType taskType, String reason);
}