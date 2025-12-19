package com.example.secondhand.service;

import com.example.secondhand.entity.Cart;
import com.example.secondhand.entity.Product;
import com.example.secondhand.repository.CartRepository;
import com.example.secondhand.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 添加商品到购物车
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     * @return 购物车项
     */
    public Cart addToCart(Long userId, Long productId, Integer quantity) {
        // 验证商品是否存在且上架
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        
        if (product.getStatus() != 1) {
            throw new RuntimeException("商品已下架，无法添加到购物车");
        }

        // 检查是否已存在该商品
        Optional<Cart> existingCart = cartRepository.findByUserIdAndProductId(userId, productId);
        
        if (existingCart.isPresent()) {
            // 如果已存在，增加数量
            Cart cart = existingCart.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            return cartRepository.save(cart);
        } else {
            // 创建新的购物车项
            Cart cart = new Cart(
                userId,
                productId,
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                quantity,
                product.getSellerId(),
                product.getSellerName()
            );
            return cartRepository.save(cart);
        }
    }

    /**
     * 获取用户购物车列表
     * @param userId 用户ID
     * @return 购物车列表
     */
    @Transactional(readOnly = true)
    public List<Cart> getCartList(Long userId) {
        return cartRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 更新购物车商品数量
     * @param cartId 购物车项ID
     * @param userId 用户ID
     * @param quantity 新数量
     * @return 更新后的购物车项
     */
    public Cart updateCartItem(Long cartId, Long userId, Integer quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));
        
        // 验证购物车项是否属于当前用户
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作此购物车项");
        }

        if (quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

        cart.setQuantity(quantity);
        return cartRepository.save(cart);
    }

    /**
     * 从购物车删除商品
     * @param cartId 购物车项ID
     * @param userId 用户ID
     */
    public void removeFromCart(Long cartId, Long userId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));
        
        // 验证购物车项是否属于当前用户
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作此购物车项");
        }

        cartRepository.delete(cart);
    }

    /**
     * 清空用户购物车
     * @param userId 用户ID
     * @return 删除的记录数
     */
    public int clearCart(Long userId) {
        return cartRepository.deleteByUserId(userId);
    }

    /**
     * 批量删除购物车商品
     * @param userId 用户ID
     * @param productIds 商品ID列表
     * @return 删除的记录数
     */
    public int removeCartItems(Long userId, List<Long> productIds) {
        return cartRepository.deleteByUserIdAndProductIdIn(userId, productIds);
    }

    /**
     * 获取购物车商品总数量
     * @param userId 用户ID
     * @return 总数量
     */
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.sumQuantityByUserId(userId);
    }

    /**
     * 计算购物车总价
     * @param userId 用户ID
     * @return 总价
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCartTotal(Long userId) {
        List<Cart> cartItems = cartRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return cartItems.stream()
                .map(Cart::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算选中商品的总价
     * @param userId 用户ID
     * @param productIds 选中的商品ID列表
     * @return 总价
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSelectedItemsTotal(Long userId, List<Long> productIds) {
        List<Cart> selectedItems = cartRepository.findByUserIdAndProductIdIn(userId, productIds);
        return selectedItems.stream()
                .map(Cart::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取选中的购物车商品
     * @param userId 用户ID
     * @param productIds 选中的商品ID列表
     * @return 购物车列表
     */
    @Transactional(readOnly = true)
    public List<Cart> getSelectedCartItems(Long userId, List<Long> productIds) {
        return cartRepository.findByUserIdAndProductIdIn(userId, productIds);
    }

    /**
     * 验证购物车商品的有效性（检查商品是否还存在且上架）
     * @param userId 用户ID
     * @return 无效的购物车项列表
     */
    @Transactional(readOnly = true)
    public List<Cart> validateCartItems(Long userId) {
        return cartRepository.findCartItemsWithUnavailableProducts(userId);
    }

    /**
     * 同步购物车商品信息（当商品信息变更时）
     * @param productId 商品ID
     */
    public void syncProductInfo(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        
        cartRepository.updateProductInfo(
            productId,
            product.getName(),
            product.getImageUrl(),
            product.getPrice()
        );
    }

    /**
     * 检查购物车中是否存在指定商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean isProductInCart(Long userId, Long productId) {
        return cartRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * 获取购物车中指定商品的数量
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 商品数量，如果不存在返回0
     */
    @Transactional(readOnly = true)
    public int getProductQuantityInCart(Long userId, Long productId) {
        Optional<Cart> cart = cartRepository.findByUserIdAndProductId(userId, productId);
        return cart.map(Cart::getQuantity).orElse(0);
    }

    /**
     * 清理无效的购物车项（商品已删除或下架）
     * @param userId 用户ID
     * @return 清理的记录数
     */
    public int cleanInvalidCartItems(Long userId) {
        List<Cart> invalidItems = cartRepository.findCartItemsWithUnavailableProducts(userId);
        if (!invalidItems.isEmpty()) {
            List<Long> invalidProductIds = invalidItems.stream()
                    .map(Cart::getProductId)
                    .toList();
            return cartRepository.deleteByUserIdAndProductIdIn(userId, invalidProductIds);
        }
        return 0;
    }
}