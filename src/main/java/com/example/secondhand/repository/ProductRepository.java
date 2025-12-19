/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-08 15:33:18
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-10 21:18:42
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\repository\ProductRepository.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.repository;

import com.example.secondhand.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * 根据卖家ID查询商品列表
     */
    List<Product> findBySellerId(Long sellerId);
    
    /**
     * 根据状态查询商品列表
     */
    List<Product> findByStatus(Integer status);
    
    /**
     * 根据卖家ID和状态查询商品列表
     */
    List<Product> findBySellerIdAndStatus(Long sellerId, Integer status);

        // ⭐ 按创建时间倒序排序（最新在最前）
    List<Product> findAllByOrderByCreateTimeDesc();
    
    /**
     * 根据名称模糊查询商品列表，按创建时间倒序排序
     */
    List<Product> findByNameContainingOrderByCreateTimeDesc(String name);
    
    /**
     * 根据名称和分类模糊查询商品列表，按创建时间倒序排序
     */
    List<Product> findByNameContainingAndCategoryContainingOrderByCreateTimeDesc(String name, String category);
    
    /**
     * 根据名称和状态查询商品列表，按创建时间倒序排序
     */
    List<Product> findByNameContainingAndStatusOrderByCreateTimeDesc(String name, Integer status);
    
    /**
     * 根据名称、分类和状态查询商品列表，按创建时间倒序排序
     */
    List<Product> findByNameContainingAndCategoryContainingAndStatusOrderByCreateTimeDesc(String name, String category, Integer status);

    // ====== 🔒 悲观锁查询方法 ======
    
    /**
     * 使用悲观锁查询商品（FOR UPDATE）
     * 用于库存操作时防止并发修改
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
    
    /**
     * 批量使用悲观锁查询商品
     * 用于批量库存操作
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findByIdsWithPessimisticLock(@Param("ids") List<Long> ids);
    
    /**
     * 使用悲观读锁查询商品
     * 用于读取时防止其他事务修改
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticReadLock(@Param("id") Long id);
}
