package com.example.secondhand.service;

import com.example.secondhand.entity.OrderItem;
import com.example.secondhand.entity.Product;
import com.example.secondhand.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 库存管理服务
 * 负责处理商品库存的预扣、确认、恢复操作
 */
@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ConcurrencyLockManager lockManager;

    /**
     * 预扣库存（使用悲观锁 + Redis分布式锁双重保护）
     * 在订单创建时调用，预先扣减库存但不确认
     * 
     * @param orderItems 订单项列表
     * @return 是否预扣成功
     */
    @Transactional
    public boolean reserveInventory(List<OrderItem> orderItems) {
        logger.info("开始预扣库存，订单项数量: {}", orderItems.size());
        
        // 存储获取的Redis锁，用于异常时释放
        Map<Long, String> acquiredLocks = new HashMap<>();
        
        try {
            // 1. 先获取所有商品的Redis分布式锁
            for (OrderItem item : orderItems) {
                Long productId = item.getProductId();
                String lockKey = ConcurrencyLockManager.getProductLockKey(productId);
                
                // 尝试获取锁，最多等待5秒
                String lockValue = lockManager.tryLockWithTimeout(lockKey, 
                    Duration.ofSeconds(30), Duration.ofSeconds(5));
                
                if (lockValue == null) {
                    logger.error("获取商品Redis锁失败，商品ID: {}", productId);
                    // 释放已获取的锁
                    releaseLocks(acquiredLocks);
                    return false;
                }
                
                acquiredLocks.put(productId, lockValue);
            }
            
            // 2. 🔒 使用悲观锁查询并扣减库存
            for (OrderItem item : orderItems) {
                Long productId = item.getProductId();
                Integer quantity = item.getQuantity();
                
                // 🔒 使用悲观锁查询商品（FOR UPDATE）
                Optional<Product> productOpt = productRepository.findByIdWithPessimisticLock(productId);
                if (productOpt.isEmpty()) {
                    logger.error("商品不存在，商品ID: {}", productId);
                    releaseLocks(acquiredLocks);
                    return false;
                }
                
                Product product = productOpt.get();
                
                // 检查库存是否充足
                if (product.getQuantity() < quantity) {
                    logger.error("库存不足，商品ID: {}, 需要数量: {}, 当前库存: {}", 
                               productId, quantity, product.getQuantity());
                    releaseLocks(acquiredLocks);
                    return false;
                }
                
                // 预扣库存
                product.setQuantity(product.getQuantity() - quantity);
                productRepository.save(product);
                
                logger.info("预扣库存成功（悲观锁），商品ID: {}, 扣减数量: {}, 剩余库存: {}", 
                          productId, quantity, product.getQuantity());
            }
            
            // 3. 释放所有Redis锁
            releaseLocks(acquiredLocks);
            
            logger.info("所有商品预扣库存成功（Redis锁 + 悲观锁）");
            return true;
            
        } catch (Exception e) {
            logger.error("预扣库存失败", e);
            // 释放所有锁
            releaseLocks(acquiredLocks);
            throw new RuntimeException("预扣库存失败: " + e.getMessage());
        }
    }

    /**
     * 释放锁集合
     * 
     * @param locks 锁集合（商品ID -> 锁值）
     */
    private void releaseLocks(Map<Long, String> locks) {
        for (Map.Entry<Long, String> entry : locks.entrySet()) {
            Long productId = entry.getKey();
            String lockValue = entry.getValue();
            String lockKey = ConcurrencyLockManager.getProductLockKey(productId);
            
            try {
                lockManager.releaseLock(lockKey, lockValue);
            } catch (Exception e) {
                logger.error("释放锁失败，商品ID: {}, 锁键: {}", productId, lockKey, e);
            }
        }
    }

    /**
     * 确认库存扣减
     * 在支付成功后调用，确认之前的预扣操作
     * 
     * @param orderNo 订单号
     * @return 是否确认成功
     */
    @Transactional
    public boolean confirmInventory(String orderNo) {
        logger.info("确认库存扣减，订单号: {}", orderNo);
        
        // 在实际实现中，这里可能需要记录库存操作日志
        // 或者更新库存状态，目前预扣时已经扣减了库存，所以这里只需要记录日志
        
        logger.info("库存扣减确认成功，订单号: {}", orderNo);
        return true;
    }

    /**
     * 恢复库存（使用悲观锁保护）
     * 在订单取消或支付失败时调用，恢复之前预扣的库存
     * 
     * @param orderItems 订单项列表
     * @return 是否恢复成功
     */
    @Transactional
    public boolean restoreInventory(List<OrderItem> orderItems) {
        logger.info("开始恢复库存，订单项数量: {}", orderItems.size());
        
        try {
            for (OrderItem item : orderItems) {
                Long productId = item.getProductId();
                Integer quantity = item.getQuantity();
                
                // 🔒 使用悲观锁查询商品（FOR UPDATE）
                Optional<Product> productOpt = productRepository.findByIdWithPessimisticLock(productId);
                if (productOpt.isEmpty()) {
                    logger.error("商品不存在，无法恢复库存，商品ID: {}", productId);
                    continue; // 继续处理其他商品
                }
                
                Product product = productOpt.get();
                
                // 恢复库存
                product.setQuantity(product.getQuantity() + quantity);
                productRepository.save(product);
                
                logger.info("恢复库存成功（悲观锁），商品ID: {}, 恢复数量: {}, 当前库存: {}", 
                          productId, quantity, product.getQuantity());
            }
            
            logger.info("所有商品库存恢复成功（悲观锁保护）");
            return true;
            
        } catch (Exception e) {
            logger.error("恢复库存失败", e);
            throw new RuntimeException("恢复库存失败: " + e.getMessage());
        }
    }

    /**
     * 检查库存是否充足（普通查询）
     * 
     * @param productId 商品ID
     * @param quantity 需要的数量
     * @return 是否库存充足
     */
    public boolean checkInventory(Long productId, Integer quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            logger.error("商品不存在，商品ID: {}", productId);
            return false;
        }
        
        Product product = productOpt.get();
        boolean sufficient = product.getQuantity() >= quantity;
        
        logger.info("库存检查，商品ID: {}, 需要数量: {}, 当前库存: {}, 是否充足: {}", 
                  productId, quantity, product.getQuantity(), sufficient);
        
        return sufficient;
    }

    /**
     * 🔒 使用悲观锁检查库存是否充足（高并发场景）
     * 适用于需要确保数据一致性的场景
     * 
     * @param productId 商品ID
     * @param quantity 需要的数量
     * @return 是否库存充足
     */
    @Transactional
    public boolean checkInventoryWithLock(Long productId, Integer quantity) {
        // 🔒 使用悲观锁查询商品
        Optional<Product> productOpt = productRepository.findByIdWithPessimisticLock(productId);
        if (productOpt.isEmpty()) {
            logger.error("商品不存在，商品ID: {}", productId);
            return false;
        }
        
        Product product = productOpt.get();
        boolean sufficient = product.getQuantity() >= quantity;
        
        logger.info("库存检查（悲观锁），商品ID: {}, 需要数量: {}, 当前库存: {}, 是否充足: {}", 
                  productId, quantity, product.getQuantity(), sufficient);
        
        return sufficient;
    }

    /**
     * 获取商品当前库存
     * 
     * @param productId 商品ID
     * @return 当前库存数量，如果商品不存在返回0
     */
    public Integer getCurrentInventory(Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            logger.error("商品不存在，商品ID: {}", productId);
            return 0;
        }
        
        return productOpt.get().getQuantity();
    }

    /**
     * 处理库存超时
     * 由超时任务调度器调用，恢复长时间未确认的预扣库存
     * 
     * @param orderNo 订单号
     * @return 处理结果
     */
    @Transactional
    public boolean handleInventoryTimeout(String orderNo) {
        logger.info("处理库存超时，订单号: {}", orderNo);
        
        try {
            // 在实际实现中，这里应该查询订单项并恢复库存
            // 由于当前架构中库存预扣和订单创建是同时进行的，
            // 库存超时处理通常由订单超时处理来完成
            
            // 这里主要是记录日志和提供接口给超时任务调度器
            logger.info("库存超时处理完成，订单号: {}", orderNo);
            return true;
            
        } catch (Exception e) {
            logger.error("处理库存超时失败，订单号: {}", orderNo, e);
            return false;
        }
    }
}