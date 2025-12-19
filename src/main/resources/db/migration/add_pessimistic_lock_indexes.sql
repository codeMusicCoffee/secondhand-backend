-- 为悲观锁优化添加索引
-- 确保 product 表的 id 字段有主键索引（通常已存在）
-- 为 quantity 字段添加索引，优化库存查询性能

-- 检查并添加 quantity 字段索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_product_quantity ON product(quantity);

-- 检查并添加 status 字段索引（如果不存在），用于过滤上架商品
CREATE INDEX IF NOT EXISTS idx_product_status ON product(status);

-- 复合索引：status + quantity，用于查询上架且有库存的商品
CREATE INDEX IF NOT EXISTS idx_product_status_quantity ON product(status, quantity);

-- 为 seller_id 添加索引（如果不存在），用于卖家商品查询
CREATE INDEX IF NOT EXISTS idx_product_seller_id ON product(seller_id);

-- 为 create_time 添加索引（如果不存在），用于按时间排序
CREATE INDEX IF NOT EXISTS idx_product_create_time ON product(create_time);

-- 注释说明
-- 这些索引将提高悲观锁查询的性能，特别是在高并发场景下
-- 主键索引确保 FOR UPDATE 锁定的效率
-- quantity 索引优化库存检查查询
-- 复合索引优化多条件查询