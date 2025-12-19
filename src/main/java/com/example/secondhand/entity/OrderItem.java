package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;              // 所属订单

    @Column(nullable = false)
    private Long productId;           // 商品ID

    @Column(nullable = false, length = 200)
    private String productName;       // 商品名称

    @Column(length = 500)
    private String productImage;      // 商品图片

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal productPrice;  // 商品单价

    @Column(nullable = false)
    private Integer quantity;         // 购买数量

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;      // 小计金额

    @Column(nullable = false)
    private Long sellerId;            // 卖家ID

    @Column(nullable = false, length = 100)
    private String sellerName;        // 卖家姓名

    // 构造函数
    public OrderItem() {}

    public OrderItem(Order order, Long productId, String productName, String productImage,
                     BigDecimal productPrice, Integer quantity, Long sellerId, String sellerName) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.subtotal = productPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
        // 重新计算小计
        if (this.quantity != null) {
            this.subtotal = productPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        // 重新计算小计
        if (this.productPrice != null) {
            this.subtotal = this.productPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    // 业务方法
    /**
     * 计算小计金额
     */
    public void calculateSubtotal() {
        if (this.productPrice != null && this.quantity != null) {
            this.subtotal = this.productPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    /**
     * 验证订单项数据的有效性
     */
    public boolean isValid() {
        return this.productId != null 
            && this.productName != null && !this.productName.trim().isEmpty()
            && this.productPrice != null && this.productPrice.compareTo(BigDecimal.ZERO) > 0
            && this.quantity != null && this.quantity > 0
            && this.sellerId != null
            && this.sellerName != null && !this.sellerName.trim().isEmpty();
    }

    @PrePersist
    @PreUpdate
    public void prePersistAndUpdate() {
        // 确保小计金额正确计算
        calculateSubtotal();
    }
}