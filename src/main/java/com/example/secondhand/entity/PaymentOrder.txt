package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体类
 * 记录支付宝支付相关的订单信息
 */
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @Column(name = "order_no", length = 50)
    private String orderNo;           // 订单号（主键）

    @Column(name = "alipay_trade_no", length = 100)
    private String alipayTradeNo;     // 支付宝交易号

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;     // 支付状态

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;        // 支付金额

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime; // 创建时间

    @Column(name = "pay_time")
    private LocalDateTime payTime;    // 支付时间

    @Column(name = "callback_data", columnDefinition = "TEXT")
    private String callbackData;      // 回调数据

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;             // 买家ID

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;         // 买家姓名

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;           // 商品标题

    @Column(name = "body", length = 500)
    private String body;              // 商品描述

    @Column(name = "timeout_express", length = 20)
    private String timeoutExpress;    // 超时时间（如：15m）

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间

    // 支付状态枚举
    public enum PaymentStatus {
        PENDING("待支付"),
        PAID("已支付"),
        FAILED("支付失败"),
        CANCELLED("已取消"),
        TIMEOUT("支付超时");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 构造函数
    public PaymentOrder() {}

    public PaymentOrder(String orderNo, BigDecimal amount, Long buyerId, String buyerName, 
                       String subject, String body) {
        this.orderNo = orderNo;
        this.amount = amount;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.subject = subject;
        this.body = body;
        this.status = PaymentStatus.PENDING;
        this.timeoutExpress = "15m";
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    // Getter 和 Setter 方法
    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getAlipayTradeNo() {
        return alipayTradeNo;
    }

    public void setAlipayTradeNo(String alipayTradeNo) {
        this.alipayTradeNo = alipayTradeNo;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTimeoutExpress() {
        return timeoutExpress;
    }

    public void setTimeoutExpress(String timeoutExpress) {
        this.timeoutExpress = timeoutExpress;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    // 业务方法
    /**
     * 检查支付状态是否可以转换到目标状态
     */
    public boolean canTransitionTo(PaymentStatus targetStatus) {
        switch (this.status) {
            case PENDING:
                return targetStatus == PaymentStatus.PAID || 
                       targetStatus == PaymentStatus.FAILED || 
                       targetStatus == PaymentStatus.CANCELLED ||
                       targetStatus == PaymentStatus.TIMEOUT;
            case PAID:
                return false; // 已支付状态不能转换到其他状态
            case FAILED:
                return targetStatus == PaymentStatus.PENDING; // 失败后可以重新支付
            case CANCELLED:
                return targetStatus == PaymentStatus.PENDING; // 取消后可以重新支付
            case TIMEOUT:
                return targetStatus == PaymentStatus.PENDING; // 超时后可以重新支付
            default:
                return false;
        }
    }

    /**
     * 转换支付状态
     */
    public void transitionTo(PaymentStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.status, newStatus));
        }
        
        PaymentStatus oldStatus = this.status;
        this.status = newStatus;
        this.updateTime = LocalDateTime.now();
        
        // 如果转换为已支付状态，记录支付时间
        if (newStatus == PaymentStatus.PAID && oldStatus != PaymentStatus.PAID) {
            this.payTime = LocalDateTime.now();
        }
    }

    /**
     * 检查支付是否已完成
     */
    public boolean isPaid() {
        return this.status == PaymentStatus.PAID;
    }

    /**
     * 检查支付是否可以取消
     */
    public boolean canCancel() {
        return this.status == PaymentStatus.PENDING;
    }

    /**
     * 检查支付是否已超时
     */
    public boolean isTimeout() {
        if (this.status != PaymentStatus.PENDING) {
            return false;
        }
        
        // 解析超时时间（默认15分钟）
        int timeoutMinutes = 15;
        if (this.timeoutExpress != null && this.timeoutExpress.endsWith("m")) {
            try {
                timeoutMinutes = Integer.parseInt(this.timeoutExpress.substring(0, this.timeoutExpress.length() - 1));
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        LocalDateTime timeoutTime = this.createTime.plusMinutes(timeoutMinutes);
        return LocalDateTime.now().isAfter(timeoutTime);
    }

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        if (this.updateTime == null) {
            this.updateTime = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.timeoutExpress == null) {
            this.timeoutExpress = "15m";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}