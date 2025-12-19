package com.example.secondhand.service;

import com.example.secondhand.entity.Cart;
import com.example.secondhand.entity.Order;
import com.example.secondhand.entity.OrderItem;
import com.example.secondhand.entity.Product;
import com.example.secondhand.entity.User;
import com.example.secondhand.entity.TimeoutTask;
import com.example.secondhand.repository.CartRepository;
import com.example.secondhand.repository.OrderRepository;
import com.example.secondhand.repository.OrderItemRepository;
import com.example.secondhand.repository.ProductRepository;
import com.example.secondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ConcurrencyLockManager lockManager;

    @Autowired
    private OrderTimeoutService orderTimeoutService;

    /**
     * 创建订单（支持前端购物车）
     * @param userId 用户ID
     * @param cartItems 购物车商品信息（包含商品ID和数量）
     * @param address 收货地址
     * @param phone 联系电话
     * @param remark 备注信息
     * @return 创建的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderFromFrontendCart(Long userId, List<Map<String, Object>> cartItems, String address, String phone, String remark) {
        // 1. 验证用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车商品信息不能为空");
        }

        // 2. 创建订单项列表用于库存管理
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        // 3. 创建订单
        String orderNo = Order.generateOrderNo();
        Order order = new Order(orderNo, userId, user.getUsername(), totalAmount, address, phone, remark);
        order = orderRepository.save(order);

        try {
            // 4. 验证商品并创建订单项
            for (Map<String, Object> item : cartItems) {
                Long productId = Long.valueOf(item.get("productId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());
                
                // 验证商品存在且可购买
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("商品不存在: " + productId));
                
                if (product.getStatus() != 1) {
                    throw new RuntimeException("商品已下架: " + product.getName());
                }

                // 创建订单项
                OrderItem orderItem = new OrderItem(
                    order,
                    product.getId(),
                    product.getName(),
                    product.getImageUrl(),
                    product.getPrice(),
                    quantity,
                    product.getSellerId(),
                    product.getSellerName()
                );
                orderItems.add(orderItem);

                // 累加总金额
                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
                totalAmount = totalAmount.add(itemTotal);
            }

            // 5. 使用库存服务预扣库存（包含并发控制）
            if (!inventoryService.reserveInventory(orderItems)) {
                throw new RuntimeException("商品库存不足");
            }

            // 6. 保存订单项
            for (OrderItem orderItem : orderItems) {
                orderItemRepository.save(orderItem);
            }

            // 7. 更新订单总金额
            order.setTotalAmount(totalAmount);
            order = orderRepository.save(order);

            // 8. 调度订单超时任务（15分钟后自动取消）
            try {
                String taskId = orderTimeoutService.scheduleOrderTimeout(orderNo, 15);
                System.out.println("订单超时任务调度成功: orderNo=" + orderNo + ", taskId=" + taskId);
            } catch (Exception e) {
                System.err.println("调度订单超时任务失败: orderNo=" + orderNo + ", error=" + e.getMessage());
                // 注意：超时任务调度失败不影响订单创建
            }

            return order;
            
        } catch (Exception e) {
            // 如果订单创建失败，恢复库存
            if (!orderItems.isEmpty()) {
                try {
                    inventoryService.restoreInventory(orderItems);
                } catch (Exception restoreException) {
                    // 记录恢复库存失败的日志，但不影响原异常抛出
                    System.err.println("恢复库存失败: " + restoreException.getMessage());
                }
            }
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建订单（兼容旧版本，基于购物车表）
     * @param userId 用户ID
     * @param productIds 商品ID列表
     * @param address 收货地址
     * @param phone 联系电话
     * @param remark 备注信息
     * @return 创建的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, List<Long> productIds, String address, String phone, String remark) {
        try {
            // 1. 验证用户
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 2. 获取购物车中的选中商品
            List<Cart> cartItems = cartRepository.findByUserIdAndProductIdIn(userId, productIds);
            if (cartItems.isEmpty()) {
                throw new RuntimeException("购物车中没有选中的商品");
            }

            // 3. 计算订单总金额
            BigDecimal totalAmount = cartItems.stream()
                    .map(Cart::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. 创建订单
            String orderNo = Order.generateOrderNo();
            Order order = new Order(orderNo, userId, user.getUsername(), totalAmount, address, phone, remark);
            order = orderRepository.save(order);

            // 5. 创建订单项
            for (Cart cartItem : cartItems) {
                OrderItem orderItem = new OrderItem(
                    order,
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getProductImage(),
                    cartItem.getProductPrice(),
                    cartItem.getQuantity(),
                    cartItem.getSellerId(),
                    cartItem.getSellerName()
                );
                orderItemRepository.save(orderItem);
            }

            // 6. 清空购物车中已购买的商品
            cartRepository.deleteByUserIdAndProductIdIn(userId, productIds);

            return order;
        } catch (Exception e) {
            // 事务会自动回滚
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取买家订单列表
     * @param buyerId 买家ID
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<Order> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreateTimeDesc(buyerId);
    }

    /**
     * 根据状态获取买家订单列表
     * @param buyerId 买家ID
     * @param status 订单状态
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<Order> getBuyerOrdersByStatus(Long buyerId, Order.OrderStatus status) {
        return orderRepository.findByBuyerIdAndStatusOrderByCreateTimeDesc(buyerId, status);
    }

    /**
     * 获取卖家销售订单列表
     * @param sellerId 卖家ID
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<Order> getSellerOrders(Long sellerId) {
        return orderRepository.findSellerOrders(sellerId);
    }

    /**
     * 根据状态获取卖家销售订单列表
     * @param sellerId 卖家ID
     * @param status 订单状态
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<Order> getSellerOrdersByStatus(Long sellerId, Order.OrderStatus status) {
        return orderRepository.findSellerOrdersByStatus(sellerId, status);
    }

    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @param userId 用户ID（用于权限验证）
     * @return 订单详情
     */
    @Transactional(readOnly = true)
    public Order getOrderDetail(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证权限：只有买家或卖家可以查看订单详情
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = orderRepository.existsByIdAndSellerId(orderId, userId);
        
        if (!isBuyer && !isSeller) {
            throw new RuntimeException("无权限查看此订单");
        }

        return order;
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 更新后的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证权限：只有买家可以取消订单
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权限取消此订单");
        }

        // 验证订单状态
        if (!order.canCancel()) {
            throw new RuntimeException("订单状态不允许取消");
        }

        try {
            // 获取订单项并恢复库存
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            if (!orderItems.isEmpty()) {
                if (!inventoryService.restoreInventory(orderItems)) {
                    throw new RuntimeException("恢复库存失败");
                }
            }

            // 更新订单状态
            order.setStatus(Order.OrderStatus.CANCELLED);
            Order savedOrder = orderRepository.save(order);

            // 取消相关的超时任务
            try {
                orderTimeoutService.cancelOrderTimeout(order.getOrderNo(), "用户主动取消订单");
                System.out.println("订单超时任务取消成功: orderNo=" + order.getOrderNo());
            } catch (Exception e) {
                System.err.println("取消订单超时任务失败: orderNo=" + order.getOrderNo() + ", error=" + e.getMessage());
            }

            return savedOrder;
            
        } catch (Exception e) {
            throw new RuntimeException("取消订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 确认收货
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 更新后的订单
     */
    public Order confirmOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证权限：只有买家可以确认收货
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权限操作此订单");
        }

        // 验证订单状态
        if (!order.canConfirm()) {
            throw new RuntimeException("订单状态不允许确认收货");
        }

        order.setStatus(Order.OrderStatus.COMPLETED);
        return orderRepository.save(order);
    }

    /**
     * 卖家发货
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return 更新后的订单
     */
    public Order shipOrder(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证权限：只有卖家可以发货
        if (!orderRepository.existsByIdAndSellerId(orderId, sellerId)) {
            throw new RuntimeException("无权限操作此订单");
        }

        // 验证订单状态
        if (!order.canShip()) {
            throw new RuntimeException("订单状态不允许发货");
        }

        order.setStatus(Order.OrderStatus.SHIPPING);
        return orderRepository.save(order);
    }

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    /**
     * 统计买家订单数量
     * @param buyerId 买家ID
     * @return 订单数量
     */
    @Transactional(readOnly = true)
    public long countBuyerOrders(Long buyerId) {
        return orderRepository.countByBuyerId(buyerId);
    }

    /**
     * 统计买家指定状态的订单数量
     * @param buyerId 买家ID
     * @param status 订单状态
     * @return 订单数量
     */
    @Transactional(readOnly = true)
    public long countBuyerOrdersByStatus(Long buyerId, Order.OrderStatus status) {
        return orderRepository.countByBuyerIdAndStatus(buyerId, status);
    }

    /**
     * 统计卖家销售订单数量
     * @param sellerId 卖家ID
     * @return 订单数量
     */
    @Transactional(readOnly = true)
    public long countSellerOrders(Long sellerId) {
        return orderRepository.countSellerOrders(sellerId);
    }

    /**
     * 统计卖家指定状态的销售订单数量
     * @param sellerId 卖家ID
     * @param status 订单状态
     * @return 订单数量
     */
    @Transactional(readOnly = true)
    public long countSellerOrdersByStatus(Long sellerId, Order.OrderStatus status) {
        return orderRepository.countSellerOrdersByStatus(sellerId, status);
    }

    /**
     * 获取订单的订单项列表
     * @param orderId 订单ID
     * @return 订单项列表
     */
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * 获取卖家在指定订单中的订单项
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return 订单项列表
     */
    @Transactional(readOnly = true)
    public List<OrderItem> getSellerOrderItems(Long orderId, Long sellerId) {
        return orderItemRepository.findSellerItemsByOrderId(orderId, sellerId);
    }

    /**
     * 验证订单权限
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param requireBuyer 是否要求买家权限
     * @return 是否有权限
     */
    @Transactional(readOnly = true)
    public boolean hasOrderPermission(Long orderId, Long userId, boolean requireBuyer) {
        if (requireBuyer) {
            return orderRepository.existsByIdAndBuyerId(orderId, userId);
        } else {
            // 买家或卖家都可以
            return orderRepository.existsByIdAndBuyerId(orderId, userId) ||
                   orderRepository.existsByIdAndSellerId(orderId, userId);
        }
    }

    /**
     * 获取指定时间范围内的订单
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(startTime, endTime);
    }

    /**
     * 处理订单超时（委托给OrderTimeoutService）
     * @param orderNo 订单号
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleOrderTimeout(String orderNo) {
        return orderTimeoutService.handleOrderTimeout(orderNo);
    }
}