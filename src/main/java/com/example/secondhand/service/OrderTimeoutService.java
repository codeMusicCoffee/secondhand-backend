package com.example.secondhand.service;

import com.example.secondhand.entity.Order;
import com.example.secondhand.entity.OrderItem;
import com.example.secondhand.entity.PaymentOrder;
import com.example.secondhand.entity.TimeoutTask;
import com.example.secondhand.repository.OrderRepository;
import com.example.secondhand.repository.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单超时处理服务
 * 专门处理订单超时相关的业务逻辑
 */
@Service
public class OrderTimeoutService {

    private static final Logger logger = LoggerFactory.getLogger(OrderTimeoutService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    @Lazy
    private TimeoutTaskManager timeoutTaskManager;

    /**
     * 为新订单调度超时任务
     * @param orderNo 订单号
     * @param timeoutMinutes 超时分钟数
     * @return 任务ID
     */
    @Transactional
    public String scheduleOrderTimeout(String orderNo, int timeoutMinutes) {
        try {
            // 验证订单存在且状态为待支付
            Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
            if (orderOpt.isEmpty()) {
                logger.error("调度订单超时任务失败：订单不存在, orderNo={}", orderNo);
                return null;
            }

            Order order = orderOpt.get();
            if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
                logger.warn("订单状态不是待支付，跳过超时任务调度: orderNo={}, status={}", 
                    orderNo, order.getStatus());
                return null;
            }

            // 调度超时任务
            String taskId = timeoutTaskManager.scheduleTimeout(
                orderNo, 
                TimeoutTask.TaskType.ORDER_TIMEOUT, 
                timeoutMinutes
            );

            if (taskId != null) {
                logger.info("订单超时任务调度成功: orderNo={}, taskId={}, timeoutMinutes={}", 
                    orderNo, taskId, timeoutMinutes);
            } else {
                logger.warn("订单超时任务调度失败: orderNo={}", orderNo);
            }

            return taskId;

        } catch (Exception e) {
            logger.error("调度订单超时任务异常: orderNo={}", orderNo, e);
            return null;
        }
    }

    /**
     * 取消订单的超时任务
     * @param orderNo 订单号
     * @param reason 取消原因
     * @return 是否成功
     */
    @Transactional
    public boolean cancelOrderTimeout(String orderNo, String reason) {
        try {
            boolean success = timeoutTaskManager.cancelTimeoutByOrder(
                orderNo, 
                TimeoutTask.TaskType.ORDER_TIMEOUT, 
                reason
            );

            if (success) {
                logger.info("订单超时任务取消成功: orderNo={}, reason={}", orderNo, reason);
            } else {
                logger.warn("订单超时任务取消失败: orderNo={}, reason={}", orderNo, reason);
            }

            return success;

        } catch (Exception e) {
            logger.error("取消订单超时任务异常: orderNo={}, reason={}", orderNo, reason, e);
            return false;
        }
    }

    /**
     * 处理订单支付成功后的超时任务取消
     * @param orderNo 订单号
     */
    @Transactional
    public void handleOrderPaid(String orderNo) {
        try {
            // 取消订单超时任务
            cancelOrderTimeout(orderNo, "订单已支付");

            logger.info("订单支付成功，超时任务已取消: orderNo={}", orderNo);

        } catch (Exception e) {
            logger.error("处理订单支付成功后的超时任务取消失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 批量处理超时订单
     * 查找所有超时的待支付订单并自动取消
     * @param timeoutMinutes 超时分钟数
     * @return 处理的订单数量
     */
    @Transactional
    public int batchProcessTimeoutOrders(int timeoutMinutes) {
        try {
            logger.info("开始批量处理超时订单，超时时间: {}分钟", timeoutMinutes);

            // 计算超时时间点
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

            // 查找超时的待支付订单
            List<Order> timeoutOrders = orderRepository.findTimeoutPendingOrders(timeoutThreshold);

            int processedCount = 0;
            for (Order order : timeoutOrders) {
                try {
                    // 直接处理超时订单
                    boolean success = handleOrderTimeout(order.getOrderNo());
                    if (success) {
                        processedCount++;
                        logger.info("超时订单处理成功: orderNo={}", order.getOrderNo());
                    } else {
                        logger.warn("超时订单处理失败: orderNo={}", order.getOrderNo());
                    }

                } catch (Exception e) {
                    logger.error("处理超时订单异常: orderNo={}", order.getOrderNo(), e);
                }
            }

            logger.info("批量处理超时订单完成: 总数={}, 成功处理={}", timeoutOrders.size(), processedCount);
            return processedCount;

        } catch (Exception e) {
            logger.error("批量处理超时订单失败", e);
            return 0;
        }
    }

    /**
     * 检查并处理单个订单的超时状态
     * @param orderNo 订单号
     * @return 处理结果
     */
    @Transactional
    public boolean checkAndProcessOrderTimeout(String orderNo) {
        try {
            Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
            if (orderOpt.isEmpty()) {
                logger.warn("检查订单超时时订单不存在: orderNo={}", orderNo);
                return false;
            }

            Order order = orderOpt.get();

            // 只处理待支付的订单
            if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
                logger.info("订单状态不是待支付，跳过超时检查: orderNo={}, status={}", 
                    orderNo, order.getStatus());
                return true;
            }

            // 检查是否超时（15分钟）
            LocalDateTime timeoutThreshold = order.getCreateTime().plusMinutes(15);
            if (LocalDateTime.now().isAfter(timeoutThreshold)) {
                // 订单已超时，直接处理
                boolean success = handleOrderTimeout(orderNo);
                
                if (success) {
                    logger.info("超时订单自动取消成功: orderNo={}", orderNo);
                } else {
                    logger.error("超时订单自动取消失败: orderNo={}", orderNo);
                }
                
                return success;
            } else {
                logger.debug("订单尚未超时: orderNo={}, createTime={}, timeoutThreshold={}", 
                    orderNo, order.getCreateTime(), timeoutThreshold);
                return true;
            }

        } catch (Exception e) {
            logger.error("检查并处理订单超时异常: orderNo={}", orderNo, e);
            return false;
        }
    }

    /**
     * 获取订单的剩余时间（分钟）
     * @param orderNo 订单号
     * @return 剩余时间，如果订单不存在或已超时返回0
     */
    public long getOrderRemainingMinutes(String orderNo) {
        try {
            Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
            if (orderOpt.isEmpty()) {
                return 0;
            }

            Order order = orderOpt.get();
            if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
                return 0;
            }

            LocalDateTime timeoutTime = order.getCreateTime().plusMinutes(15);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(timeoutTime)) {
                return 0; // 已超时
            }

            return java.time.Duration.between(now, timeoutTime).toMinutes();

        } catch (Exception e) {
            logger.error("获取订单剩余时间失败: orderNo={}", orderNo, e);
            return 0;
        }
    }

    /**
     * 获取订单超时状态信息
     * @param orderNo 订单号
     * @return 超时状态信息
     */
    public OrderTimeoutInfo getOrderTimeoutInfo(String orderNo) {
        try {
            Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
            if (orderOpt.isEmpty()) {
                return new OrderTimeoutInfo(false, false, 0, "订单不存在");
            }

            Order order = orderOpt.get();
            boolean isPendingPayment = order.getStatus() == Order.OrderStatus.PENDING_PAYMENT;
            
            if (!isPendingPayment) {
                return new OrderTimeoutInfo(true, false, 0, 
                    "订单状态：" + order.getStatus().getDescription());
            }

            LocalDateTime timeoutTime = order.getCreateTime().plusMinutes(15);
            LocalDateTime now = LocalDateTime.now();
            boolean isTimeout = now.isAfter(timeoutTime);
            long remainingMinutes = isTimeout ? 0 : 
                java.time.Duration.between(now, timeoutTime).toMinutes();

            String message = isTimeout ? "订单已超时" : 
                String.format("剩余 %d 分钟", remainingMinutes);

            return new OrderTimeoutInfo(true, isTimeout, remainingMinutes, message);

        } catch (Exception e) {
            logger.error("获取订单超时信息失败: orderNo={}", orderNo, e);
            return new OrderTimeoutInfo(false, false, 0, "获取信息失败：" + e.getMessage());
        }
    }

    /**
     * 订单超时信息类
     */
    public static class OrderTimeoutInfo {
        private final boolean exists;
        private final boolean isTimeout;
        private final long remainingMinutes;
        private final String message;

        public OrderTimeoutInfo(boolean exists, boolean isTimeout, long remainingMinutes, String message) {
            this.exists = exists;
            this.isTimeout = isTimeout;
            this.remainingMinutes = remainingMinutes;
            this.message = message;
        }

        public boolean isExists() { return exists; }
        public boolean isTimeout() { return isTimeout; }
        public long getRemainingMinutes() { return remainingMinutes; }
        public String getMessage() { return message; }
    }

    @Autowired
    private PaymentStatusChecker paymentStatusChecker;

    /**
     * 处理订单超时
     * 由超时任务调度器调用，自动取消超时的订单
     * @param orderNo 订单号
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleOrderTimeout(String orderNo) {
        try {
            // 查询订单
            Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
            if (orderOpt.isEmpty()) {
                logger.error("处理订单超时时订单不存在: " + orderNo);
                return false;
            }

            Order order = orderOpt.get();
            
            // 检查订单状态，只有待支付的订单才能超时取消
            if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
                logger.info("订单状态不是待支付，跳过超时处理: orderNo={}, status={}", 
                    orderNo, order.getStatus());
                return true;
            }

            // 检查订单是否真的超时（15分钟）
            LocalDateTime timeoutThreshold = order.getCreateTime().plusMinutes(15);
            if (LocalDateTime.now().isBefore(timeoutThreshold)) {
                logger.info("订单尚未超时，跳过处理: orderNo={}", orderNo);
                return true;
            }

            // 🔥 关键修复：在取消订单前，先检查支付状态，防止取消已支付的订单
            try {
                Optional<PaymentOrder> paymentOrderOpt = paymentStatusChecker.getPaymentOrder(orderNo);
                if (paymentOrderOpt.isPresent()) {
                    PaymentOrder paymentOrder = paymentOrderOpt.get();
                    if (paymentOrder.isPaid()) {
                        logger.warn("订单超时处理时发现订单已支付，跳过取消: orderNo={}, paymentStatus={}", 
                            orderNo, paymentOrder.getStatus());
                        
                        // 如果订单已支付但状态仍是待付款，更新为待发货
                        if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT) {
                            order.setStatus(Order.OrderStatus.PENDING_SHIPMENT);
                            orderRepository.save(order);
                            logger.info("订单超时处理时发现已支付，已更新状态为待发货: orderNo={}", orderNo);
                        }
                        
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.warn("检查支付状态失败，继续超时处理: orderNo={}, error={}", orderNo, e.getMessage());
            }

            // 获取订单项并恢复库存
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            if (!orderItems.isEmpty()) {
                if (!inventoryService.restoreInventory(orderItems)) {
                    logger.error("恢复库存失败，但继续取消订单: orderNo={}", orderNo);
                    // 注意：即使恢复库存失败，也要继续取消订单，避免订单状态不一致
                }
            }

            // 更新订单状态为已取消
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);

            logger.info("订单超时自动取消成功: orderNo={}, 创建时间={}, 取消时间={}, 恢复库存商品数={}", 
                orderNo, order.getCreateTime(), LocalDateTime.now(), orderItems.size());
            return true;

        } catch (Exception e) {
            logger.error("处理订单超时失败: orderNo={}", orderNo, e);
            return false;
        }
    }

    /**
     * 重新调度所有待支付订单的超时任务
     * 在系统启动时调用
     * @return 调度的任务数量
     */
    @Transactional
    public int rescheduleAllPendingOrderTimeouts() {
        try {
            logger.info("开始重新调度所有待支付订单的超时任务...");

            // 查询最近1小时内创建的待支付订单
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusHours(1);
            
            List<Order> pendingOrders = orderRepository.findPendingOrdersByTimeRange(startTime, now);
            
            int scheduledCount = 0;
            int skippedCount = 0;
            
            for (Order order : pendingOrders) {
                try {
                    // 计算剩余超时时间
                    LocalDateTime timeoutTime = order.getCreateTime().plusMinutes(15);
                    
                    if (now.isAfter(timeoutTime)) {
                        // 订单已超时，直接处理
                        boolean success = handleOrderTimeout(order.getOrderNo());
                        if (success) {
                            logger.info("启动时处理超时订单: orderNo={}", order.getOrderNo());
                        }
                        skippedCount++;
                        continue;
                    }
                    
                    // 计算剩余分钟数
                    long remainingMinutes = java.time.Duration.between(now, timeoutTime).toMinutes();
                    if (remainingMinutes <= 0) {
                        remainingMinutes = 1; // 至少1分钟
                    }
                    
                    // 重新调度超时任务
                    String taskId = scheduleOrderTimeout(order.getOrderNo(), (int) remainingMinutes);
                    if (taskId != null) {
                        scheduledCount++;
                        logger.debug("重新调度订单超时任务: orderNo={}, remainingMinutes={}, taskId={}", 
                            order.getOrderNo(), remainingMinutes, taskId);
                    }
                    
                } catch (Exception e) {
                    logger.error("重新调度订单超时任务失败: orderNo={}", order.getOrderNo(), e);
                }
            }

            logger.info("待支付订单超时任务重新调度完成: 总订单数={}, 成功调度={}, 跳过/处理={}", 
                pendingOrders.size(), scheduledCount, skippedCount);
            
            return scheduledCount;
            
        } catch (Exception e) {
            logger.error("重新调度所有待支付订单超时任务失败", e);
            return 0;
        }
    }
}