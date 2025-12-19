package com.example.secondhand.controller;

import com.example.secondhand.common.Result;
import com.example.secondhand.entity.Order;
import com.example.secondhand.entity.OrderItem;
import com.example.secondhand.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单（支持前端购物车）
     */
    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String address = request.get("address").toString();
            String phone = request.get("phone").toString();
            String remark = request.get("remark") != null ? request.get("remark").toString() : "";

            // 检查是否有购物车商品信息（新版本）
            if (request.containsKey("cartItems")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cartItems = (List<Map<String, Object>>) request.get("cartItems");
                Order order = orderService.createOrderFromFrontendCart(userId, cartItems, address, phone, remark);
                return Result.success(order);
            } 
            // 兼容旧版本（基于商品ID列表）
            else if (request.containsKey("productIds")) {
                @SuppressWarnings("unchecked")
                List<Long> productIds = (List<Long>) request.get("productIds");
                Order order = orderService.createOrder(userId, productIds, address, phone, remark);
                return Result.success(order);
            } 
            else {
                throw new RuntimeException("缺少商品信息");
            }
        } catch (Exception e) {
            return Result.error( e.getMessage());
        }
    }

    /**
     * 获取我的订单列表（买家视角）
     */
    @GetMapping("/my")
    public Result<List<Order>> getMyOrders(@RequestParam Long userId, 
                                          @RequestParam(required = false) String status) {
        try {
            List<Order> orders;
            if (status != null && !status.equals("all")) {
                // 处理状态映射，兼容旧的PENDING状态
                String normalizedStatus = status.toUpperCase();
                if ("PENDING".equals(normalizedStatus)) {
                    normalizedStatus = "PENDING_PAYMENT";
                }
                
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(normalizedStatus);
                orders = orderService.getBuyerOrdersByStatus(userId, orderStatus);
            } else {
                orders = orderService.getBuyerOrders(userId);
            }
            return Result.success(orders);
        } catch (IllegalArgumentException e) {
            return Result.error("无效的订单状态: " + status);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的销售订单（卖家视角）
     */
    @GetMapping("/sales")
    public Result<List<Order>> getMySales(@RequestParam Long userId, 
                                         @RequestParam(required = false) String status) {
        try {
            List<Order> orders;
            if (status != null && !status.equals("all")) {
                // 处理状态映射，兼容旧的PENDING状态
                String normalizedStatus = status.toUpperCase();
                if ("PENDING".equals(normalizedStatus)) {
                    normalizedStatus = "PENDING_PAYMENT";
                }
                
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(normalizedStatus);
                orders = orderService.getSellerOrdersByStatus(userId, orderStatus);
            } else {
                orders = orderService.getSellerOrders(userId);
            }
            return Result.success(orders);
        } catch (IllegalArgumentException e) {
            return Result.error("无效的订单状态: " + status);
        } catch (Exception e) {
            return Result.error("获取销售订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<Order> getOrderDetail(@PathVariable Long id, @RequestParam Long userId) {
        try {
            Order order = orderService.getOrderDetail(id, userId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel/{id}")
    public Result<Order> cancelOrder(@PathVariable Long id, @RequestParam Long userId) {
        try {
            Order order = orderService.cancelOrder(id, userId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    /**
     * 确认收货
     */
    @PutMapping("/confirm/{id}")
    public Result<Order> confirmOrder(@PathVariable Long id, @RequestParam Long userId) {
        try {
            Order order = orderService.confirmOrder(id, userId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("确认收货失败: " + e.getMessage());
        }
    }

    /**
     * 卖家发货
     */
    @PutMapping("/ship/{id}")
    public Result<Order> shipOrder(@PathVariable Long id, @RequestParam Long sellerId) {
        try {
            Order order = orderService.shipOrder(id, sellerId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("发货失败: " + e.getMessage());
        }
    }

    /**
     * 根据订单号查询订单
     */
    @GetMapping("/by-order-no/{orderNo}")
    public Result<Order> getOrderByOrderNo(@PathVariable String orderNo, @RequestParam Long userId) {
        try {
            Order order = orderService.getOrderByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
            
            // 验证权限
            if (!orderService.hasOrderPermission(order.getId(), userId, false)) {
                throw new RuntimeException("无权限查看此订单");
            }
            
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单项列表
     */
    @GetMapping("/{id}/items")
    public Result<List<OrderItem>> getOrderItems(@PathVariable Long id, @RequestParam Long userId) {
        try {
            // 验证权限
            if (!orderService.hasOrderPermission(id, userId, false)) {
                throw new RuntimeException("无权限查看此订单");
            }
            
            List<OrderItem> items = orderService.getOrderItems(id);
            return Result.success(items);
        } catch (Exception e) {
            return Result.error("获取订单项失败: " + e.getMessage());
        }
    }

    /**
     * 获取卖家在指定订单中的订单项
     */
    @GetMapping("/{id}/seller-items")
    public Result<List<OrderItem>> getSellerOrderItems(@PathVariable Long id, @RequestParam Long sellerId) {
        try {
            List<OrderItem> items = orderService.getSellerOrderItems(id, sellerId);
            return Result.success(items);
        } catch (Exception e) {
            return Result.error("获取卖家订单项失败: " + e.getMessage());
        }
    }

    /**
     * 统计买家订单数量
     */
    @GetMapping("/count/buyer")
    public Result<Map<String, Long>> getBuyerOrderCount(@RequestParam Long userId) {
        try {
            long totalCount = orderService.countBuyerOrders(userId);
            long pendingPaymentCount = orderService.countBuyerOrdersByStatus(userId, Order.OrderStatus.PENDING_PAYMENT);
            long pendingShipmentCount = orderService.countBuyerOrdersByStatus(userId, Order.OrderStatus.PENDING_SHIPMENT);
            long shippingCount = orderService.countBuyerOrdersByStatus(userId, Order.OrderStatus.SHIPPING);
            long completedCount = orderService.countBuyerOrdersByStatus(userId, Order.OrderStatus.COMPLETED);
            long cancelledCount = orderService.countBuyerOrdersByStatus(userId, Order.OrderStatus.CANCELLED);
            
            Map<String, Long> result = Map.of(
                "total", totalCount,
                "pendingPayment", pendingPaymentCount,
                "pendingShipment", pendingShipmentCount,
                "shipping", shippingCount,
                "completed", completedCount,
                "cancelled", cancelledCount
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("统计订单数量失败: " + e.getMessage());
        }
    }

    /**
     * 统计卖家销售订单数量
     */
    @GetMapping("/count/seller")
    public Result<Map<String, Long>> getSellerOrderCount(@RequestParam Long userId) {
        try {
            long totalCount = orderService.countSellerOrders(userId);
            long pendingPaymentCount = orderService.countSellerOrdersByStatus(userId, Order.OrderStatus.PENDING_PAYMENT);
            long pendingShipmentCount = orderService.countSellerOrdersByStatus(userId, Order.OrderStatus.PENDING_SHIPMENT);
            long shippingCount = orderService.countSellerOrdersByStatus(userId, Order.OrderStatus.SHIPPING);
            long completedCount = orderService.countSellerOrdersByStatus(userId, Order.OrderStatus.COMPLETED);
            long cancelledCount = orderService.countSellerOrdersByStatus(userId, Order.OrderStatus.CANCELLED);
            
            Map<String, Long> result = Map.of(
                "total", totalCount,
                "pendingPayment", pendingPaymentCount,
                "pendingShipment", pendingShipmentCount,
                "shipping", shippingCount,
                "completed", completedCount,
                "cancelled", cancelledCount
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("统计销售订单数量失败: " + e.getMessage());
        }
    }

    /**
     * 检查订单权限
     */
    @GetMapping("/{id}/check-permission")
    public Result<Map<String, Boolean>> checkOrderPermission(@PathVariable Long id, 
                                                            @RequestParam Long userId,
                                                            @RequestParam(defaultValue = "false") boolean requireBuyer) {
        try {
            boolean hasPermission = orderService.hasOrderPermission(id, userId, requireBuyer);
            Map<String, Boolean> result = Map.of("hasPermission", hasPermission);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("检查权限失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单状态列表（用于前端下拉选择）
     */
    @GetMapping("/status-list")
    public Result<List<Map<String, String>>> getOrderStatusList() {
        try {
            List<Map<String, String>> statusList = List.of(
                Map.of("value", "PENDING_PAYMENT", "label", "待付款"),
                Map.of("value", "PENDING_SHIPMENT", "label", "待发货"),
                Map.of("value", "SHIPPING", "label", "配送中"),
                Map.of("value", "COMPLETED", "label", "已完成"),
                Map.of("value", "CANCELLED", "label", "已取消")
            );
            return Result.success(statusList);
        } catch (Exception e) {
            return Result.error("获取状态列表失败: " + e.getMessage());
        }
    }
}