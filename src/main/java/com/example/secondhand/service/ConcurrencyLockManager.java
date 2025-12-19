package com.example.secondhand.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 并发锁管理器
 * 使用Redis实现分布式锁，防止超卖等并发问题
 */
@Service
public class ConcurrencyLockManager {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyLockManager.class);
    
    private static final String LOCK_PREFIX = "inventory_lock:";
    private static final long DEFAULT_TIMEOUT_SECONDS = 30; // 默认锁超时时间30秒
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // Lua脚本用于原子性释放锁
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";

    /**
     * 获取锁
     * 
     * @param lockKey 锁的键
     * @return 锁的值（用于释放锁时验证），如果获取失败返回null
     */
    public String acquireLock(String lockKey) {
        return acquireLock(lockKey, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    /**
     * 获取锁（带超时时间）
     * 
     * @param lockKey 锁的键
     * @param timeout 锁的超时时间
     * @return 锁的值（用于释放锁时验证），如果获取失败返回null
     */
    public String acquireLock(String lockKey, Duration timeout) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();
        
        try {
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockValue, timeout.getSeconds(), TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(success)) {
                logger.info("获取锁成功，锁键: {}, 锁值: {}, 超时时间: {}秒", 
                          fullKey, lockValue, timeout.getSeconds());
                return lockValue;
            } else {
                logger.warn("获取锁失败，锁键: {}", fullKey);
                return null;
            }
        } catch (Exception e) {
            logger.error("获取锁异常，锁键: {}", fullKey, e);
            // Redis连接失败时，返回一个特殊的锁值，表示降级处理
            // 在没有Redis的情况下，我们仍然允许操作继续，但会记录警告
            logger.warn("Redis不可用，降级处理：允许操作继续但无分布式锁保护");
            return "FALLBACK_" + lockValue;
        }
    }
    /**
     * 尝试获取锁（带重试）
     * 
     * @param lockKey 锁的键
     * @param timeout 锁的超时时间
     * @param retryTimeout 重试超时时间
     * @return 锁的值（用于释放锁时验证），如果获取失败返回null
     */
    public String tryLockWithTimeout(String lockKey, Duration timeout, Duration retryTimeout) {
        long startTime = System.currentTimeMillis();
        long retryTimeoutMs = retryTimeout.toMillis();
        
        while (System.currentTimeMillis() - startTime < retryTimeoutMs) {
            String lockValue = acquireLock(lockKey, timeout);
            if (lockValue != null) {
                return lockValue;
            }
            
            // 等待一小段时间后重试
            try {
                Thread.sleep(50); // 50ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("等待锁时被中断，锁键: {}", lockKey);
                return null;
            }
        }
        
        logger.warn("获取锁超时，锁键: {}, 重试时间: {}ms", lockKey, retryTimeoutMs);
        return null;
    }

    /**
     * 释放锁
     * 
     * @param lockKey 锁的键
     * @param lockValue 锁的值（用于验证锁的所有权）
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        String fullKey = LOCK_PREFIX + lockKey;
        
        // 如果是降级处理的锁值，直接返回成功
        if (lockValue != null && lockValue.startsWith("FALLBACK_")) {
            logger.info("释放降级锁成功，锁键: {}, 锁值: {}", fullKey, lockValue);
            return true;
        }
        
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, 
                Collections.singletonList(fullKey), lockValue);
            
            boolean success = result != null && result == 1L;
            
            if (success) {
                logger.info("释放锁成功，锁键: {}, 锁值: {}", fullKey, lockValue);
            } else {
                logger.warn("释放锁失败，锁键: {}, 锁值: {}", fullKey, lockValue);
            }
            
            return success;
        } catch (Exception e) {
            logger.error("释放锁异常，锁键: {}, 锁值: {}", fullKey, lockValue, e);
            // Redis连接失败时，也认为释放成功（降级处理）
            logger.warn("Redis不可用，降级处理：假设锁释放成功");
            return true;
        }
    }

    /**
     * 强制释放锁（危险操作，仅在特殊情况下使用）
     * 
     * @param lockKey 锁的键
     * @return 是否释放成功
     */
    public boolean forceReleaseLock(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        
        try {
            Boolean result = redisTemplate.delete(fullKey);
            boolean success = Boolean.TRUE.equals(result);
            
            if (success) {
                logger.warn("强制释放锁成功，锁键: {}", fullKey);
            } else {
                logger.warn("强制释放锁失败，锁键: {}", fullKey);
            }
            
            return success;
        } catch (Exception e) {
            logger.error("强制释放锁异常，锁键: {}", fullKey, e);
            return false;
        }
    }

    /**
     * 检查锁是否存在
     * 
     * @param lockKey 锁的键
     * @return 锁是否存在
     */
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        
        try {
            Boolean exists = redisTemplate.hasKey(fullKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("检查锁状态异常，锁键: {}", fullKey, e);
            return false;
        }
    }

    /**
     * 获取商品库存锁的键
     * 
     * @param productId 商品ID
     * @return 锁的键
     */
    public static String getProductLockKey(Long productId) {
        return "product:" + productId;
    }

    /**
     * 获取订单锁的键
     * 
     * @param orderNo 订单号
     * @return 锁的键
     */
    public static String getOrderLockKey(String orderNo) {
        return "order:" + orderNo;
    }
}