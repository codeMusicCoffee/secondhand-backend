package com.example.secondhand.event;

import org.springframework.context.ApplicationEvent;

/**
 * 支付成功事件
 * 用于解耦支付服务和订单超时服务之间的循环依赖
 */
public class PaymentSuccessEvent extends ApplicationEvent {
    
    private final String orderNo;
    private final String alipayTradeNo;
    private final String tradeStatus;
    
    public PaymentSuccessEvent(Object source, String orderNo, String alipayTradeNo, String tradeStatus) {
        super(source);
        this.orderNo = orderNo;
        this.alipayTradeNo = alipayTradeNo;
        this.tradeStatus = tradeStatus;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public String getAlipayTradeNo() {
        return alipayTradeNo;
    }
    
    public String getTradeStatus() {
        return tradeStatus;
    }
}