/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-18 09:51:36
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-18 09:58:48
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\config\AsyncConfig.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置
 * 用于支持异步事件处理
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Event-");
        executor.initialize();
        return executor;
    }
}