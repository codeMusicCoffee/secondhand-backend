/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-10 16:15:29
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-10 16:15:35
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\entity\ProductComment.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product_comment")
public class ProductComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String userName;

    private String content;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // ====== 回复功能相关字段 ======
    
    @Column(name = "parent_id")
    private Long parentId;  // 父评论ID，NULL表示顶级评论
    
    @Column(name = "reply_to_user_id")
    private Long replyToUserId;  // 回复的用户ID
    
    @Column(name = "reply_to_user_name")
    private String replyToUserName;  // 回复的用户名
    
    @Column(name = "reply_count")
    private Integer replyCount = 0;  // 回复数量
    
    @Column(name = "comment_level")
    private Integer commentLevel = 0;  // 评论层级，0为顶级评论

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @com.fasterxml.jackson.annotation.JsonIgnore  // 避免循环引用
    private Product product;

    // 子回复列表（用于前端展示，不持久化到数据库）
    @Transient
    private List<ProductComment> replies;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.replyCount == null) {
            this.replyCount = 0;
        }
        if (this.commentLevel == null) {
            this.commentLevel = 0;
        }
    }

    // ====== Getter / Setter ======

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreateTime() { return createTime; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    // ====== 回复功能 Getter / Setter ======

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long replyToUserId) { this.replyToUserId = replyToUserId; }

    public String getReplyToUserName() { return replyToUserName; }
    public void setReplyToUserName(String replyToUserName) { this.replyToUserName = replyToUserName; }

    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }

    public Integer getCommentLevel() { return commentLevel; }
    public void setCommentLevel(Integer commentLevel) { this.commentLevel = commentLevel; }

    public List<ProductComment> getReplies() { return replies; }
    public void setReplies(List<ProductComment> replies) { this.replies = replies; }
}
