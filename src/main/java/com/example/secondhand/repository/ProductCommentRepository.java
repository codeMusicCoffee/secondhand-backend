package com.example.secondhand.repository;

import com.example.secondhand.entity.ProductComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCommentRepository extends JpaRepository<ProductComment, Long> {

    // 获取商品的所有评论（扁平）
    List<ProductComment> findByProduct_Id(Long productId);

    // 获取顶级评论（parentId = null）
    List<ProductComment> findByProduct_IdAndParentIdIsNull(Long productId);

    // 获取某条评论的所有回复
    List<ProductComment> findByParentIdOrderByCreateTimeAsc(Long parentId);

    /**
     * 获取商品的所有评论，按层级结构排序
     * Hibernate 6 兼容写法：
     * - parentId 为 null 的优先
     * - 同一父级下按 parentId、createTime 排序
     */
    @Query("""
           SELECT c FROM ProductComment c
           WHERE c.product.id = :productId
           ORDER BY
               (CASE WHEN c.parentId IS NULL THEN 0 ELSE 1 END),
               COALESCE(c.parentId, c.id),
               c.createTime ASC
           """)
    List<ProductComment> findByProductIdOrderByHierarchy(@Param("productId") Long productId);

    // 统计回复数量
    @Query("SELECT COUNT(c) FROM ProductComment c WHERE c.parentId = :parentId")
    Long countRepliesByParentId(@Param("parentId") Long parentId);
}
