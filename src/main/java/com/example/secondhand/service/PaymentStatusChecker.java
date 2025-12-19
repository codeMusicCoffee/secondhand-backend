package com.example.secondhand.service;

import com.example.secondhand.entity.PaymentOrder;
import com.example.secondhand.repository.PaymentOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 支付状态检查服务
 * 用于避免OrderTimeoutService和PaymentService之间的循环依赖
 */
@Service
public class PaymentStatusChecker {
    
    @Autowired
    private PaymentOrderRepository paymentOrderRepository;
    
    /**
     * 检查订单是否已支付
     * @param orderNo 订单号
     * @return 是否已支付
     */
    @Transactional(readOnly = true)
    public boolean isOrderPaid(String orderNo) {
        try {
            Optional<PaymentOrder> paymentOrderOpt = paymentOrderRepository.findById(orderNo);
            return paymentOrderOpt.isPresent() && paymentOrderOpt.get().isPaid();
        } catch (Exception e) {
            // 出现异常时返回false，让超时处理继续进行
            return false;
        }
    }
    
    /**
     * 获取支付订单状态
     * @param orderNo 订单号
     * @return 支付订单（如果存在）
     */
    @Transactional(readOnly = true)
    public Optional<PaymentOrder> getPaymentOrder(String orderNo) {
        try {
            return paymentOrderRepository.findById(orderNo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}