package com.example.secondhand.repository;

import com.example.secondhand.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 根据用户ID查询购物车列表
     * @param userId 用户ID
     * @return 购物车列表
     */
    List<Cart> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据用户ID和商品ID查询购物车项
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 购物车项
     */
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * 根据用户ID和商品ID列表查询购物车项
     * @param userId 用户ID
     * @param productIds 商品ID列表
     * @return 购物车列表
     */
    List<Cart> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

    /**
     * 根据用户ID统计购物车商品数量
     * @param userId 用户ID
     * @return 商品数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户ID计算购物车商品总数量（考虑每个商品的数量）
     * @param userId 用户ID
     * @return 总数量
     */
    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM Cart c WHERE c.userId = :userId")
    int sumQuantityByUserId(@Param("userId") Long userId);

    /**
     * 检查用户购物车中是否存在指定商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否存在
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /**
     * 根据用户ID删除所有购物车项
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和商品ID列表批量删除购物车项
     * @param userId 用户ID
     * @param productIds 商品ID列表
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.userId = :userId AND c.productId IN :productIds")
    int deleteByUserIdAndProductIdIn(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);

    /**
     * 根据商品ID删除所有相关的购物车项（商品下架时使用）
     * @param productId 商品ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.productId = :productId")
    int deleteByProductId(@Param("productId") Long productId);

    /**
     * 根据卖家ID查询包含其商品的购物车项
     * @param sellerId 卖家ID
     * @return 购物车列表
     */
    List<Cart> findBySellerIdOrderByCreateTimeDesc(Long sellerId);

    /**
     * 根据用户ID和卖家ID查询购物车项
     * @param userId 用户ID
     * @param sellerId 卖家ID
     * @return 购物车列表
     */
    List<Cart> findByUserIdAndSellerIdOrderByCreateTimeDesc(Long userId, Long sellerId);

    /**
     * 更新购物车项的商品信息（当商品信息变更时同步更新）
     * @param productId 商品ID
     * @param productName 商品名称
     * @param productImage 商品图片
     * @param productPrice 商品价格
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE Cart c SET c.productName = :productName, c.productImage = :productImage, " +
           "c.productPrice = :productPrice WHERE c.productId = :productId")
    int updateProductInfo(@Param("productId") Long productId,
                         @Param("productName") String productName,
                         @Param("productImage") String productImage,
                         @Param("productPrice") java.math.BigDecimal productPrice);

    /**
     * 查找购物车中价格发生变化的商品
     * @param userId 用户ID
     * @return 购物车列表
     */
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND EXISTS " +
           "(SELECT p FROM Product p WHERE p.id = c.productId AND p.price != c.productPrice)")
    List<Cart> findCartItemsWithPriceChanges(@Param("userId") Long userId);

    /**
     * 查找购物车中已下架的商品
     * @param userId 用户ID
     * @return 购物车列表
     */
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND EXISTS " +
           "(SELECT p FROM Product p WHERE p.id = c.productId AND p.status = 0)")
    List<Cart> findCartItemsWithUnavailableProducts(@Param("userId") Long userId);

    /**
     * 检查购物车项是否属于指定用户
     * @param cartId 购物车项ID
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByIdAndUserId(Long cartId, Long userId);
}