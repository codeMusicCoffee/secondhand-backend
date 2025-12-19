package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;              // 用户ID

    @Column(name = "product_id", nullable = false)
    private Long productId;           // 商品ID

    @Column(nullable = false, length = 200)
    private String productName;       // 商品名称

    @Column(length = 500)
    private String productImage;      // 商品图片

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal productPrice;  // 商品价格

    @Column(nullable = false)
    private Integer quantity;         // 数量

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;            // 卖家ID

    @Column(name = "seller_name", nullable = false, length = 100)
    private String sellerName;        // 卖家姓名

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime; // 添加时间

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间

    // 构造函数
    public Cart() {}

    public Cart(Long userId, Long productId, String productName, String productImage,
                BigDecimal productPrice, Integer quantity, Long sellerId, String sellerName) {
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updateTime = LocalDateTime.now();
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

    // 业务方法
    /**
     * 计算该购物车项的总价
     */
    public BigDecimal getTotalPrice() {
        if (this.productPrice != null && this.quantity != null) {
            return this.productPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 增加商品数量
     */
    public void increaseQuantity(int amount) {
        if (amount > 0) {
            this.quantity += amount;
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 减少商品数量
     */
    public void decreaseQuantity(int amount) {
        if (amount > 0 && this.quantity > amount) {
            this.quantity -= amount;
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 验证购物车项数据的有效性
     */
    public boolean isValid() {
        return this.userId != null 
            && this.productId != null
            && this.productName != null && !this.productName.trim().isEmpty()
            && this.productPrice != null && this.productPrice.compareTo(BigDecimal.ZERO) > 0
            && this.quantity != null && this.quantity > 0
            && this.sellerId != null
            && this.sellerName != null && !this.sellerName.trim().isEmpty();
    }

    /**
     * 检查是否是同一个商品（用户ID和商品ID相同）
     */
    public boolean isSameProduct(Long userId, Long productId) {
        return this.userId.equals(userId) && this.productId.equals(productId);
    }

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        if (this.updateTime == null) {
            this.updateTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}