-- 高频查询索引优化

-- 申领单按状态查询（列表筛选最常用）
CREATE INDEX IF NOT EXISTS idx_requisitions_status ON requisitions(status);

-- 申领单按科室+状态联合查询
CREATE INDEX IF NOT EXISTS idx_requisitions_dept_status ON requisitions(dept_id, status);

-- 库存按耗材ID查询（发放时FEFO查询、统计等高频场景）
CREATE INDEX IF NOT EXISTS idx_inventory_material_id ON inventory(material_id);

-- 库存按状态筛选（有效库存查询）
CREATE INDEX IF NOT EXISTS idx_inventory_status ON inventory(status);

-- 出入库流水按科室查询（科室消耗排名）
CREATE INDEX IF NOT EXISTS idx_inv_transactions_dept ON inventory_transactions(dept_id);

-- 出入库流水按类型+时间查询（报表统计）
CREATE INDEX IF NOT EXISTS idx_inv_transactions_type_time ON inventory_transactions(transaction_type, create_time);

-- 出入库流水按耗材查询
CREATE INDEX IF NOT EXISTS idx_inv_transactions_material ON inventory_transactions(material_id);

-- 科室二级库按科室查询
CREATE INDEX IF NOT EXISTS idx_dept_inventory_dept ON dept_inventory(dept_id);

-- 盘点记录按科室+状态查询
CREATE INDEX IF NOT EXISTS idx_dept_stocktaking_dept_status ON dept_stocktaking(dept_id, status);
