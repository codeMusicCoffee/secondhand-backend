package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 支付日志实体类
 * 记录支付操作的详细信息，用于问题排查和数据分析
 */
@Entity
@Table(name = "payment_logs", indexes = {
    @Index(name = "idx_order_no", columnList = "order_no"),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "idx_operation", columnList = "operation")
})
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;           // 订单号

    @Column(name = "alipay_trade_no", length = 100)
    private String alipayTradeNo;     // 支付宝交易号

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LogOperation operation;   // 操作类型

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;       // 请求数据

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;      // 响应数据

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;      // 错误信息

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogLevel level;           // 日志级别

    @Column(name = "user_id")
    private Long userId;              // 用户ID

    @Column(name = "ip_address", length = 50)
    private String ipAddress;         // IP地址

    @Column(name = "user_agent", length = 500)
    private String userAgent;         // 用户代理

    @Column(name = "execution_time")
    private Long executionTime;       // 执行时间（毫秒）

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime; // 创建时间

    // 日志操作类型枚举
    public enum LogOperation {
        PAYMENT_INIT("支付发起"),
        PAYMENT_CALLBACK("支付回调"),
        PAYMENT_QUERY("支付查询"),
        PAYMENT_CANCEL("支付取消"),
        STATUS_UPDATE("状态更新"),
        TIMEOUT_CHECK("超时检查"),
        INVENTORY_RESERVE("库存预扣"),
        INVENTORY_CONFIRM("库存确认"),
        INVENTORY_RESTORE("库存恢复"),
        ORDER_CREATE("订单创建"),
        ORDER_UPDATE("订单更新"),
        SIGNATURE_VERIFY("签名验证"),
        ERROR_HANDLE("错误处理"),
        TIMEOUT_HANDLE("超时处理"),
        CALLBACK_TIMEOUT("回调超时");

        private final String description;

        LogOperation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 日志级别枚举
    public enum LogLevel {
        INFO("信息"),
        WARN("警告"),
        ERROR("错误"),
        DEBUG("调试");

        private final String description;

        LogLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 构造函数
    public PaymentLog() {}

    public PaymentLog(String orderNo, LogOperation operation, LogLevel level) {
        this.orderNo = orderNo;
        this.operation = operation;
        this.level = level;
        this.createTime = LocalDateTime.now();
    }

    public PaymentLog(String orderNo, LogOperation operation, LogLevel level, 
                     String requestData, String responseData) {
        this(orderNo, operation, level);
        this.requestData = requestData;
        this.responseData = responseData;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LogOperation getOperation() {
        return operation;
    }

    public void setOperation(LogOperation operation) {
        this.operation = operation;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    // 业务方法
    /**
     * 创建信息级别日志
     */
    public static PaymentLog info(String orderNo, LogOperation operation, String message) {
        PaymentLog log = new PaymentLog(orderNo, operation, LogLevel.INFO);
        log.setResponseData(message);
        return log;
    }

    /**
     * 创建警告级别日志
     */
    public static PaymentLog warn(String orderNo, LogOperation operation, String message) {
        PaymentLog log = new PaymentLog(orderNo, operation, LogLevel.WARN);
        log.setErrorMessage(message);
        return log;
    }

    /**
     * 创建错误级别日志
     */
    public static PaymentLog error(String orderNo, LogOperation operation, String errorMessage) {
        PaymentLog log = new PaymentLog(orderNo, operation, LogLevel.ERROR);
        log.setErrorMessage(errorMessage);
        return log;
    }

    /**
     * 创建错误级别日志（包含异常信息）
     */
    public static PaymentLog error(String orderNo, LogOperation operation, Exception exception) {
        PaymentLog log = new PaymentLog(orderNo, operation, LogLevel.ERROR);
        log.setErrorMessage(exception.getMessage());
        
        // 记录异常堆栈信息
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(exception.getClass().getSimpleName()).append(": ").append(exception.getMessage());
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.append("\n\tat ").append(element.toString());
            if (stackTrace.length() > 4000) { // 限制长度
                stackTrace.append("\n\t... (truncated)");
                break;
            }
        }
        log.setResponseData(stackTrace.toString());
        return log;
    }

    /**
     * 创建调试级别日志
     */
    public static PaymentLog debug(String orderNo, LogOperation operation, String requestData, String responseData) {
        return new PaymentLog(orderNo, operation, LogLevel.DEBUG, requestData, responseData);
    }

    /**
     * 设置请求上下文信息
     */
    public PaymentLog withContext(Long userId, String ipAddress, String userAgent) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        return this;
    }

    /**
     * 设置支付宝交易号
     */
    public PaymentLog withAlipayTradeNo(String alipayTradeNo) {
        this.alipayTradeNo = alipayTradeNo;
        return this;
    }

    /**
     * 设置执行时间
     */
    public PaymentLog withExecutionTime(long startTime) {
        this.executionTime = System.currentTimeMillis() - startTime;
        return this;
    }

    /**
     * 检查是否为错误日志
     */
    public boolean isError() {
        return this.level == LogLevel.ERROR;
    }

    /**
     * 检查是否为警告日志
     */
    public boolean isWarn() {
        return this.level == LogLevel.WARN;
    }

    /**
     * 获取简短描述
     */
    public String getShortDescription() {
        return String.format("[%s] %s - %s", 
            this.level.name(), 
            this.operation.getDescription(),
            this.orderNo);
    }

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
    }
}