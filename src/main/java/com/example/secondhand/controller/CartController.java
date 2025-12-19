package com.example.secondhand.controller;

import com.example.secondhand.common.Result;
import com.example.secondhand.entity.Cart;
import com.example.secondhand.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@CrossOrigin
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 获取购物车列表
     */
    @GetMapping("/list")
    public Result<List<Cart>> getCartList(@RequestParam Long userId) {
        try {
            List<Cart> cartList = cartService.getCartList(userId);
            return Result.success(cartList);
        } catch (Exception e) {
            return Result.<List<Cart>>error("获取购物车列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<Cart> addToCart(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());

            Cart cart = cartService.addToCart(userId, productId, quantity);
            return Result.success(cart);
        } catch (Exception e) {
            return Result.error("添加到购物车失败: " + e.getMessage());
        }
    }

    /**
     * 更新购物车商品数量
     */
    @PutMapping("/update/{id}")
    public Result<Cart> updateCartItem(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());

            Cart cart = cartService.updateCartItem(id, userId, quantity);
            return Result.success(cart);
        } catch (Exception e) {
            return Result.error("更新购物车失败: " + e.getMessage());
        }
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/remove/{id}")
    public Result<String> removeFromCart(@PathVariable Long id, @RequestParam Long userId) {
        try {
            cartService.removeFromCart(id, userId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public Result<String> clearCart(@RequestParam Long userId) {
        try {
            int deletedCount = cartService.clearCart(userId);
            return Result.success("清空成功，删除了 " + deletedCount + " 个商品");
        } catch (Exception e) {
            return Result.error("清空购物车失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除购物车商品
     */
    @DeleteMapping("/remove-batch")
    public Result<String> removeCartItems(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            @SuppressWarnings("unchecked")
            List<Long> productIds = (List<Long>) request.get("productIds");

            int deletedCount = cartService.removeCartItems(userId, productIds);
            return Result.success("删除成功，删除了 " + deletedCount + " 个商品");
        } catch (Exception e) {
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取购物车商品总数量
     */
    @GetMapping("/count")
    public Result<Integer> getCartItemCount(@RequestParam Long userId) {
        try {
            int count = cartService.getCartItemCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("获取购物车数量失败: " + e.getMessage());
        }
    }

    /**
     * 计算购物车总价
     */
    @GetMapping("/total")
    public Result<BigDecimal> calculateCartTotal(@RequestParam Long userId) {
        try {
            BigDecimal total = cartService.calculateCartTotal(userId);
            return Result.success(total);
        } catch (Exception e) {
            return Result.error("计算总价失败: " + e.getMessage());
        }
    }

    /**
     * 计算选中商品的总价
     */
    @PostMapping("/calculate-selected")
    public Result<BigDecimal> calculateSelectedItemsTotal(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            @SuppressWarnings("unchecked")
            List<Long> productIds = (List<Long>) request.get("productIds");

            BigDecimal total = cartService.calculateSelectedItemsTotal(userId, productIds);
            return Result.success(total);
        } catch (Exception e) {
            return Result.error("计算选中商品总价失败: " + e.getMessage());
        }
    }

    /**
     * 获取选中的购物车商品
     */
    @PostMapping("/selected-items")
    public Result<List<Cart>> getSelectedCartItems(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            @SuppressWarnings("unchecked")
            List<Long> productIds = (List<Long>) request.get("productIds");

            List<Cart> selectedItems = cartService.getSelectedCartItems(userId, productIds);
            return Result.success(selectedItems);
        } catch (Exception e) {
            return Result.error("获取选中商品失败: " + e.getMessage());
        }
    }

    /**
     * 验证购物车商品的有效性
     */
    @GetMapping("/validate")
    public Result<List<Cart>> validateCartItems(@RequestParam Long userId) {
        try {
            List<Cart> invalidItems = cartService.validateCartItems(userId);
            return Result.success(invalidItems);
        } catch (Exception e) {
            return Result.error("验证购物车失败: " + e.getMessage());
        }
    }

    /**
     * 检查商品是否在购物车中
     */
    @GetMapping("/check-product")
    public Result<Map<String, Object>> checkProductInCart(@RequestParam Long userId, @RequestParam Long productId) {
        try {
            boolean inCart = cartService.isProductInCart(userId, productId);
            int quantity = cartService.getProductQuantityInCart(userId, productId);
            
            Map<String, Object> result = Map.of(
                "inCart", inCart,
                "quantity", quantity
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("检查商品失败: " + e.getMessage());
        }
    }

    /**
     * 清理无效的购物车项
     */
    @DeleteMapping("/clean-invalid")
    public Result<String> cleanInvalidCartItems(@RequestParam Long userId) {
        try {
            int cleanedCount = cartService.cleanInvalidCartItems(userId);
            return Result.success("清理完成，删除了 " + cleanedCount + " 个无效商品");
        } catch (Exception e) {
            return Result.error("清理无效商品失败: " + e.getMessage());
        }
    }

    /**
     * 同步商品信息
     */
    @PostMapping("/sync-product/{productId}")
    public Result<String> syncProductInfo(@PathVariable Long productId) {
        try {
            cartService.syncProductInfo(productId);
            return Result.success("同步成功");
        } catch (Exception e) {
            return Result.error("同步商品信息失败: " + e.getMessage());
        }
    }
}