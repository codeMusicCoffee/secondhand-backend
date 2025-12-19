package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNo;           // 订单号

    @Column(nullable = false)
    private Long buyerId;             // 买家ID

    @Column(nullable = false)
    private String buyerName;         // 买家姓名

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;   // 订单总金额

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;       // 订单状态

    @Column(nullable = false, length = 500)
    private String address;           // 收货地址

    @Column(nullable = false, length = 20)
    private String phone;             // 联系电话

    @Column(length = 200)
    private String remark;            // 备注信息

    @Column(nullable = false)
    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;    // 订单项列表

    // 订单状态枚举
    public enum OrderStatus {
        PENDING_PAYMENT("待付款"),
        PENDING_SHIPMENT("待发货"), 
        SHIPPING("配送中"),
        COMPLETED("已完成"),
        CANCELLED("已取消");

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 构造函数
    public Order() {}

    public Order(String orderNo, Long buyerId, String buyerName, BigDecimal totalAmount, 
                 String address, String phone, String remark) {
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.totalAmount = totalAmount;
        this.address = address;
        this.phone = phone;
        this.remark = remark;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    // 业务方法
    /**
     * 生成订单号
     * 格式：ORD + 年月日时分秒 + 3位随机数
     */
    public static String generateOrderNo() {
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        int random = (int) (Math.random() * 1000);
        return String.format("ORD%s%03d", timestamp, random);
    }

    /**
     * 检查订单状态是否可以取消
     */
    public boolean canCancel() {
        return this.status == OrderStatus.PENDING_PAYMENT || this.status == OrderStatus.PENDING_SHIPMENT;
    }

    /**
     * 检查订单状态是否可以发货
     */
    public boolean canShip() {
        return this.status == OrderStatus.PENDING_SHIPMENT;
    }

    /**
     * 检查订单状态是否可以确认收货
     */
    public boolean canConfirm() {
        return this.status == OrderStatus.SHIPPING;
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
            this.status = OrderStatus.PENDING_PAYMENT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}