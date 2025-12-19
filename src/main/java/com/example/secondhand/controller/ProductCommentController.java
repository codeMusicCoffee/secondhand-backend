/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-10 16:16:13
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-10 22:15:19
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\controller\ProductCommentController.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%配%E7%BD%AE
 */
package com.example.secondhand.controller;

import com.example.secondhand.common.Result;
import com.example.secondhand.entity.Product;
import com.example.secondhand.entity.ProductComment;
import com.example.secondhand.repository.ProductCommentRepository;
import com.example.secondhand.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comment")
@CrossOrigin
public class ProductCommentController {

    private final ProductCommentRepository commentRepository;
    private final ProductRepository productRepository;

    public ProductCommentController(ProductCommentRepository commentRepository,
                                    ProductRepository productRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
    }

    // ====== 创建评论 ======
    @PostMapping("/add")
    public Result<ProductComment> addComment(@RequestParam Long productId,
                                           @RequestParam Long userId,
                                           @RequestParam String userName,
                                           @RequestParam String content,
                                           @RequestParam(required = false) Long parentId,
                                           @RequestParam(required = false) Long replyToUserId,
                                           @RequestParam(required = false) String replyToUserName) {
        try {
            // 参数验证
            if (productId == null || userId == null || userName == null || content == null) {
                return Result.error("参数不能为空");
            }
            
            if (content.trim().isEmpty()) {
                return Result.error("评论内容不能为空");
            }
            
            if (content.length() > 500) {
                return Result.error("评论内容不能超过500字符");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            ProductComment comment = new ProductComment();
            comment.setUserId(userId);
            comment.setUserName(userName.trim());
            comment.setContent(content.trim());
            comment.setProduct(product);
            
            // 简化回复逻辑：所有回复都指向主评论
            if (parentId != null) {
                // 查找根评论（如果回复的是回复，则找到最顶层的主评论）
                ProductComment parentComment = commentRepository.findById(parentId)
                        .orElseThrow(() -> new RuntimeException("父评论不存在"));
                
                // 如果父评论本身就是回复，则找到它的根评论
                Long rootCommentId = parentComment.getParentId() != null ? parentComment.getParentId() : parentId;
                
                comment.setParentId(rootCommentId);  // 始终指向根评论
                comment.setReplyToUserId(replyToUserId);
                comment.setReplyToUserName(replyToUserName != null ? replyToUserName.trim() : null);
                comment.setCommentLevel(1);  // 所有回复都是1级
            } else {
                // 顶级评论
                comment.setParentId(null);
                comment.setCommentLevel(0);
            }

            ProductComment savedComment = commentRepository.save(comment);
            
            // 更新根评论的回复数量
            if (parentId != null) {
                Long rootCommentId = comment.getParentId();
                updateReplyCount(rootCommentId);
            }
            
            return Result.success(savedComment);
            
        } catch (Exception e) {
            return Result.error("发表评论失败: " + e.getMessage());
        }
    }

    // ====== 获取商品评论（扁平化结构） ======
    @GetMapping("/list")
    public Result<List<ProductComment>> getComments(@RequestParam Long productId) {
        try {
            if (productId == null) {
                return Result.error("商品ID不能为空");
            }
            
            // 验证商品是否存在
            if (!productRepository.existsById(productId)) {
                return Result.error("商品不存在");
            }
            
            // 获取所有评论
            List<ProductComment> allComments = commentRepository.findByProduct_Id(productId);
            
            // 构建简化的层级结构：主评论 + 扁平化回复
            List<ProductComment> structuredComments = buildFlatCommentStructure(allComments);
            
            return Result.success(structuredComments);
            
        } catch (Exception e) {
            return Result.error("获取评论列表失败: " + e.getMessage());
        }
    }

    // ====== 获取商品的顶级评论 ======
    @GetMapping("/top-level")
    public Result<List<ProductComment>> getTopLevelComments(@RequestParam Long productId) {
        try {
            if (productId == null) {
                return Result.error("商品ID不能为空");
            }
            
            List<ProductComment> topComments = commentRepository.findByProduct_IdAndParentIdIsNull(productId);
            return Result.success(topComments);
            
        } catch (Exception e) {
            return Result.error("获取顶级评论失败: " + e.getMessage());
        }
    }

    // ====== 获取评论的回复 ======
    @GetMapping("/replies")
    public Result<List<ProductComment>> getReplies(@RequestParam Long parentId) {
        try {
            if (parentId == null) {
                return Result.error("父评论ID不能为空");
            }
            
            List<ProductComment> replies = commentRepository.findByParentIdOrderByCreateTimeAsc(parentId);
            return Result.success(replies);
            
        } catch (Exception e) {
            return Result.error("获取回复列表失败: " + e.getMessage());
        }
    }

    // ====== 删除评论 ======
    @DeleteMapping("/delete")
    public Result<String> deleteComment(@RequestParam Long commentId) {
        try {
            if (commentId == null) {
                return Result.error("评论ID不能为空");
            }
            
            ProductComment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("评论不存在"));
            
            Long parentId = comment.getParentId();
            
            if (parentId == null) {
                // 删除主评论时，同时删除所有回复
                List<ProductComment> replies = commentRepository.findByParentIdOrderByCreateTimeAsc(commentId);
                for (ProductComment reply : replies) {
                    commentRepository.deleteById(reply.getId());
                }
            }
            
            // 删除评论本身
            commentRepository.deleteById(commentId);
            
            // 更新父评论的回复数量
            if (parentId != null) {
                updateReplyCount(parentId);
            }
            
            return Result.success("删除评论成功");
            
        } catch (Exception e) {
            return Result.error("删除评论失败: " + e.getMessage());
        }
    }

    // ====== 获取评论统计信息 ======
    @GetMapping("/stats")
    public Result<Object> getCommentStats(@RequestParam Long productId) {
        try {
            if (productId == null) {
                return Result.error("商品ID不能为空");
            }
            
            // 获取所有评论
            List<ProductComment> allComments = commentRepository.findByProduct_Id(productId);
            
            // 统计信息
            long totalComments = allComments.size();
            long mainComments = allComments.stream().filter(c -> c.getParentId() == null).count();
            long replies = totalComments - mainComments;
            
            // 构建统计结果
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalComments", totalComments);
            stats.put("mainComments", mainComments);
            stats.put("replies", replies);
            stats.put("productId", productId);
            
            return Result.success(stats);
            
        } catch (Exception e) {
            return Result.error("获取评论统计失败: " + e.getMessage());
        }
    }

    // ====== 私有方法：构建扁平化评论结构 ======
    private List<ProductComment> buildFlatCommentStructure(List<ProductComment> allComments) {
        // 分离主评论和回复
        List<ProductComment> mainComments = allComments.stream()
                .filter(comment -> comment.getParentId() == null)
                .sorted((c1, c2) -> c1.getCreateTime().compareTo(c2.getCreateTime()))
                .collect(Collectors.toList());
        
        // 为每个主评论添加其所有回复（扁平化）
        for (ProductComment mainComment : mainComments) {
            List<ProductComment> replies = allComments.stream()
                    .filter(comment -> mainComment.getId().equals(comment.getParentId()))
                    .sorted((c1, c2) -> c1.getCreateTime().compareTo(c2.getCreateTime()))
                    .collect(Collectors.toList());
            
            mainComment.setReplies(replies);
            mainComment.setReplyCount(replies.size());
        }
        
        return mainComments;
    }

    // ====== 私有方法：更新回复数量 ======
    private void updateReplyCount(Long parentId) {
        Long replyCount = commentRepository.countRepliesByParentId(parentId);
        ProductComment parentComment = commentRepository.findById(parentId).orElse(null);
        if (parentComment != null) {
            parentComment.setReplyCount(replyCount.intValue());
            commentRepository.save(parentComment);
        }
    }
}
