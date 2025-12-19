package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    private String description;

    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    private Integer status; // 1上架 0下架

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // ====== 新增字段 ======
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "condition_level")
    private Integer conditionLevel; // 1~5（全新→8成新）
    
    // 商品数量
    @Column(nullable = false, name = "quantity")
    private Integer quantity; // 商品数量

    // ====== 评论（一对多） ======
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore  // 避免循环引用
    private List<ProductComment> comments;

    // ====== Getter / Setter ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public List<ProductComment> getComments() { return comments; }
    public void setComments(List<ProductComment> comments) { this.comments = comments; }

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.status = 1;
        this.quantity = 1; // 默认商品数量为1
    }
}