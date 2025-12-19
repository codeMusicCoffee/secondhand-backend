package com.example.secondhand.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 任务调度器配置
 * 配置Spring Task Scheduler用于超时任务调度
 */
@Configuration
@EnableScheduling
public class TaskSchedulerConfig {

    /**
     * 配置任务调度器
     * @return TaskScheduler实例
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // 设置线程池大小
        scheduler.setPoolSize(10);
        
        // 设置线程名前缀
        scheduler.setThreadNamePrefix("timeout-task-");
        
        // 设置等待任务完成后再关闭线程池
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        
        // 设置等待时间（秒）
        scheduler.setAwaitTerminationSeconds(60);
        
        // 设置拒绝策略：由调用者线程执行
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 初始化
        scheduler.initialize();
        
        return scheduler;
    }
}