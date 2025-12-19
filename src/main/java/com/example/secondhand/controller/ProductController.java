/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-08 15:33:38
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-12 14:00:16
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\controller\ProductController.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.controller;

import com.example.secondhand.entity.Product;
import com.example.secondhand.repository.ProductRepository;
import com.example.secondhand.common.Result;   // ✅ 一定要有这行
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/product")
@CrossOrigin   // ✅ 解决前端跨域问题
public class ProductController {

    @Resource
    private ProductRepository productRepository;

    // ✅ 1. 新增商品
    @PostMapping("/add")
    public Result<Product> add(@RequestBody Product product, HttpServletRequest request) {
        try {
            // 从请求中获取用户ID
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error("用户未登录或token无效");
            }
            
            // 设置卖家信息
            product.setSellerId(userId);
            if (product.getSellerName() == null || product.getSellerName().isEmpty()) {
                product.setSellerName("用户" + userId); // 使用用户ID作为默认卖家名称
            }
            if (product.getStatus() == null) {
                product.setStatus(1); // 默认上架
            }
            
            System.out.println("🛍️ 创建商品: " + product.getName());
            System.out.println("💰 价格: " + product.getPrice());
            System.out.println("👤 卖家: " + product.getSellerName() + " (ID: " + product.getSellerId() + ")");
            
            Product save = productRepository.save(product);
            System.out.println("✅ 商品创建成功，ID: " + save.getId());
            
            return Result.success(save);
        } catch (Exception e) {
            System.err.println("❌ 商品创建失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("商品创建失败: " + e.getMessage());
        }
    }

    // ✅ 2. 修改商品
    @PostMapping("/update")
    public Result<Product> update(@RequestBody Product product) {
        System.out.println("📦 更新商品: " + product.getId());
        System.out.println("🔢 商品数量: " + product.getQuantity());
        
        // 先查询数据库中现有的商品信息，确保保留原有的创建时间
        Product existingProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        
        // 保留原有的创建时间
        product.setCreateTime(existingProduct.getCreateTime());
        
        Product save = productRepository.save(product);
        return Result.success(save);
    }

    // ✅ 3. 删除商品
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        productRepository.deleteById(id);
        return Result.success("删除成功");
    }

    // ✅ 4. 商品列表 - 所有人都可以查看，不根据token筛选
@GetMapping("/list")
public Result<List<Product>> list(
        @RequestParam(required = false) String name, 
        @RequestParam(required = false) String category, 
        @RequestParam(required = false) Integer status) {

    List<Product> list;

    // 根据筛选条件查询商品
    if (name != null && !name.isEmpty() && category != null && !category.isEmpty() && status != null) {
        list = productRepository.findByNameContainingAndCategoryContainingAndStatusOrderByCreateTimeDesc(name, category, status);
    } else if (name != null && !name.isEmpty() && category != null && !category.isEmpty()) {
        list = productRepository.findByNameContainingAndCategoryContainingOrderByCreateTimeDesc(name, category);
    } else if (name != null && !name.isEmpty() && status != null) {
        list = productRepository.findByNameContainingAndStatusOrderByCreateTimeDesc(name, status);
    } else if (category != null && !category.isEmpty() && status != null) {
        list = productRepository.findByNameContainingAndCategoryContainingAndStatusOrderByCreateTimeDesc("", category, status);
    } else if (name != null && !name.isEmpty()) {
        list = productRepository.findByNameContainingOrderByCreateTimeDesc(name);
    } else if (category != null && !category.isEmpty()) {
        list = productRepository.findByNameContainingAndCategoryContainingOrderByCreateTimeDesc("", category);
    } else if (status != null) {
        list = productRepository.findByStatus(status);
    } else {
        list = productRepository.findAllByOrderByCreateTimeDesc();
    }

    System.out.println("查询到的商品数量: " + list.size());
    return Result.success(list);
}

    @GetMapping("/manage")
public Result<List<Product>> manage(HttpServletRequest request,
                                  @RequestParam(required = false) Integer status) {

    // 从请求中获取用户ID
    Long userId = (Long) request.getAttribute("userId");
    System.out.println("获取到的用户ID: " + userId);
    if (userId == null) {
        return Result.error("用户未登录或token无效");
    }

    List<Product> list;

    // 只返回当前登录用户的商品
    if (status != null) {
        list = productRepository.findBySellerIdAndStatus(userId, status);
    } else {
        list = productRepository.findBySellerId(userId);
    }

    System.out.println("查询到的商品数量: " + list.size());
    return Result.success(list);
}

    // ✅ 4.1 获取商品详情
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        return Result.success(product);
    }

    // ✅ 4.2 获取用户商品列表
    @GetMapping("/user/{userId}")
    public Result<List<Product>> getUserProducts(@PathVariable Long userId) {
        List<Product> products = productRepository.findBySellerId(userId);
        return Result.success(products);
    }

    // ✅ 5. 上架 / 下架
    @PostMapping("/status/{id}/{status}")
    public Result<String> changeStatus(@PathVariable Long id,
                                       @PathVariable Integer status) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            // 验证状态值
            if (status != 0 && status != 1) {
                return Result.error("无效的状态值，只能是0（下架）或1（上架）");
            }
            
            String oldStatusText = product.getStatus() == 1 ? "上架" : "下架";
            String newStatusText = status == 1 ? "上架" : "下架";
            
            product.setStatus(status);
            productRepository.save(product);
            
            System.out.println("📦 商品状态变更: " + product.getName() + " 从 " + oldStatusText + " 变更为 " + newStatusText);
            
            return Result.success("商品" + newStatusText + "成功");
        } catch (Exception e) {
            System.err.println("❌ 商品状态变更失败: " + e.getMessage());
            return Result.error("状态修改失败: " + e.getMessage());
        }
    }

    // ✅ 5.1 单独上架接口
    @PostMapping("/online/{id}")
    public Result<String> onlineProduct(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            if (product.getStatus() == 1) {
                return Result.error("商品已经是上架状态");
            }
            
            product.setStatus(1);
            productRepository.save(product);
            
            System.out.println("📦 商品上架: " + product.getName());
            
            return Result.success("商品上架成功");
        } catch (Exception e) {
            System.err.println("❌ 商品上架失败: " + e.getMessage());
            return Result.error("商品上架失败: " + e.getMessage());
        }
    }

    // ✅ 5.2 单独下架接口
    @PostMapping("/offline/{id}")
    public Result<String> offlineProduct(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            if (product.getStatus() == 0) {
                return Result.error("商品已经是下架状态");
            }
            
            product.setStatus(0);
            productRepository.save(product);
            
            System.out.println("📦 商品下架: " + product.getName());
            
            return Result.success("商品下架成功");
        } catch (Exception e) {
            System.err.println("❌ 商品下架失败: " + e.getMessage());
            return Result.error("商品下架失败: " + e.getMessage());
        }
    }

    // ✅ 5.3 批量上架接口
    @PostMapping("/batch/online")
    public Result<String> batchOnlineProducts(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("商品ID列表不能为空");
            }
            
            List<Product> products = productRepository.findAllById(ids);
            if (products.isEmpty()) {
                return Result.error("未找到指定的商品");
            }
            
            int updatedCount = 0;
            for (Product product : products) {
                if (product.getStatus() == 0) {
                    product.setStatus(1);
                    productRepository.save(product);
                    updatedCount++;
                    System.out.println("📦 批量上架: " + product.getName());
                }
            }
            
            return Result.success("成功上架 " + updatedCount + " 个商品");
        } catch (Exception e) {
            System.err.println("❌ 批量上架失败: " + e.getMessage());
            return Result.error("批量上架失败: " + e.getMessage());
        }
    }

    // ✅ 5.4 批量下架接口
    @PostMapping("/batch/offline")
    public Result<String> batchOfflineProducts(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("商品ID列表不能为空");
            }
            
            List<Product> products = productRepository.findAllById(ids);
            if (products.isEmpty()) {
                return Result.error("未找到指定的商品");
            }
            
            int updatedCount = 0;
            for (Product product : products) {
                if (product.getStatus() == 1) {
                    product.setStatus(0);
                    productRepository.save(product);
                    updatedCount++;
                    System.out.println("📦 批量下架: " + product.getName());
                }
            }
            
            return Result.success("成功下架 " + updatedCount + " 个商品");
        } catch (Exception e) {
            System.err.println("❌ 批量下架失败: " + e.getMessage());
            return Result.error("批量下架失败: " + e.getMessage());
        }
    }
}