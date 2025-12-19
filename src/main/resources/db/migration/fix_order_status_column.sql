-- 修复订单状态字段长度问题
-- 将 status 字段长度从 varchar(20) 扩展到 varchar(50)

USE demo;

-- 1. 查看当前表结构
-- DESCRIBE orders;

-- 2. 修改 status 字段长度
ALTER TABLE orders 
MODIFY COLUMN status varchar(50) NOT NULL DEFAULT 'PENDING_PAYMENT' 
COMMENT '订单状态：PENDING_PAYMENT-待付款，PENDING_SHIPMENT-待发货，SHIPPING-配送中，COMPLETED-已完成，CANCELLED-已取消';

-- 3. 验证修改结果
-- DESCRIBE orders;

-- 4. 查看当前数据
-- SELECT id, order_no, status, create_time FROM orders ORDER BY create_time DESC LIMIT 10;