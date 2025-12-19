-- 修复订单状态数据库记录
-- 将旧的 PENDING 状态更新为 PENDING_PAYMENT

-- 1. 查看当前数据库中的订单状态分布
-- SELECT status, COUNT(*) as count FROM orders GROUP BY status;

-- 2. 将所有 PENDING 状态的订单更新为 PENDING_PAYMENT
UPDATE orders 
SET status = 'PENDING_PAYMENT' 
WHERE status = 'PENDING';

-- 3. 验证更新结果
-- SELECT status, COUNT(*) as count FROM orders GROUP BY status;

-- 注意：如果你的表名不是 'orders'，请相应修改表名