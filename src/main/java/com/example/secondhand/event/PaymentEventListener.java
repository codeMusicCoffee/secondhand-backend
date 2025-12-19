package com.example.secondhand.event;

import com.example.secondhand.service.OrderTimeoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 支付事件监听器
 * 处理支付相关的事件，避免服务间的循环依赖
 */
@Component
public class PaymentEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);
    
    @Autowired
    private OrderTimeoutService orderTimeoutService;
    
    /**
     * 处理支付成功事件
     * 取消订单超时任务
     */
    @EventListener
    @Async
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        try {
            logger.info("收到支付成功事件: orderNo={}, alipayTradeNo={}", 
                event.getOrderNo(), event.getAlipayTradeNo());
            
            // 取消订单超时任务
            boolean success = orderTimeoutService.cancelOrderTimeout(
                event.getOrderNo(), 
                "支付成功，交易号：" + event.getAlipayTradeNo()
            );
            
            if (success) {
                logger.info("支付成功事件处理完成，超时任务已取消: orderNo={}", event.getOrderNo());
            } else {
                logger.warn("支付成功事件处理失败，超时任务取消失败: orderNo={}", event.getOrderNo());
            }
            
        } catch (Exception e) {
            logger.error("处理支付成功事件异常: orderNo={}", event.getOrderNo(), e);
        }
    }
}