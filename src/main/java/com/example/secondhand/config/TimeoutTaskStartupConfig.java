package com.example.secondhand.config;

import com.example.secondhand.service.OrderTimeoutService;
import com.example.secondhand.service.TimeoutTaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 超时任务启动配置
 * 在应用启动时恢复未完成的超时任务
 */
@Component
public class TimeoutTaskStartupConfig implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutTaskStartupConfig.class);

    @Autowired
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @Autowired
    private OrderTimeoutService orderTimeoutService;

    /**
     * 应用启动后执行
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        logger.info("应用启动完成，开始恢复超时任务...");
        
        try {
            // 1. 恢复数据库中未完成的超时任务
            recoverDatabaseTasks();
            
            // 2. 处理系统宕机期间可能超时的订单
            processStaleOrders();
            
            logger.info("超时任务恢复完成");
            
        } catch (Exception e) {
            logger.error("恢复超时任务失败", e);
        }
    }

    /**
     * 恢复数据库中未完成的超时任务
     */
    private void recoverDatabaseTasks() {
        try {
            logger.info("开始恢复数据库中的未完成任务...");
            
            // TimeoutTaskScheduler的initialize方法会在@PostConstruct时自动调用
            // 这里主要是记录日志和监控
            
            // 获取任务统计信息
            TimeoutTaskScheduler.TaskStatistics stats = timeoutTaskScheduler.getTaskStatistics();
            
            logger.info("任务恢复统计: " +
                "当前调度任务={}, 当前执行中任务={}, 当前重试任务={}, " +
                "历史调度总数={}, 历史执行总数={}, 历史失败总数={}",
                stats.getCurrentScheduled(),
                stats.getCurrentExecuting(), 
                stats.getCurrentRetry(),
                stats.getTotalScheduled(),
                stats.getTotalExecuted(),
                stats.getTotalFailed());
                
        } catch (Exception e) {
            logger.error("恢复数据库任务失败", e);
        }
    }

    /**
     * 处理系统宕机期间可能超时的订单
     */
    private void processStaleOrders() {
        try {
            logger.info("开始处理系统宕机期间的超时订单...");
            
            // 1. 批量处理超时订单（15分钟超时）
            int processedCount = orderTimeoutService.batchProcessTimeoutOrders(15);
            
            if (processedCount > 0) {
                logger.info("处理了 {} 个超时订单", processedCount);
            } else {
                logger.info("没有发现超时订单");
            }
            
            // 2. 重新调度所有待支付订单的超时任务
            int rescheduledCount = orderTimeoutService.rescheduleAllPendingOrderTimeouts();
            
            if (rescheduledCount > 0) {
                logger.info("重新调度了 {} 个待支付订单的超时任务", rescheduledCount);
            } else {
                logger.info("没有需要重新调度的待支付订单");
            }
            
        } catch (Exception e) {
            logger.error("处理超时订单失败", e);
        }
    }
}