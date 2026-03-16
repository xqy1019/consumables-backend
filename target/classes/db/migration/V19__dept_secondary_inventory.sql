-- V19: 科室二级库存 + 科室盘点 + 自动补货
-- 实现"期初库存 + 本期领用 - 期末盘点 = 实际消耗"的消耗倒推

-- 科室二级库存（每个科室有自己的耗材库存）
CREATE TABLE dept_inventory (
    id BIGSERIAL PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    current_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    last_stocktaking_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(dept_id, material_id)
);

-- 科室盘点单
CREATE TABLE dept_stocktaking (
    id BIGSERIAL PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS, COMPLETED
    total_consumption DECIMAL(10,2) DEFAULT 0,           -- 本次盘点计算出的总消耗量
    note TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 科室盘点明细
CREATE TABLE dept_stocktaking_items (
    id BIGSERIAL PRIMARY KEY,
    stocktaking_id BIGINT NOT NULL REFERENCES dept_stocktaking(id),
    material_id BIGINT NOT NULL,
    system_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,    -- 系统账面数量
    actual_quantity DECIMAL(10,2),                        -- 实际盘点数量
    consumption DECIMAL(10,2),                            -- 消耗量 = system - actual（正数=正常消耗）
    note VARCHAR(200)
);

-- 自动补货记录
CREATE TABLE auto_replenishment_log (
    id BIGSERIAL PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    current_quantity DECIMAL(10,2) NOT NULL,
    min_quantity DECIMAL(10,2) NOT NULL,
    replenish_quantity DECIMAL(10,2) NOT NULL,            -- 建议补货量 = par_quantity - current_quantity
    requisition_id BIGINT,                                -- 自动生成的申领单ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_dept_inv_dept ON dept_inventory(dept_id);
CREATE INDEX idx_dept_inv_material ON dept_inventory(material_id);
CREATE INDEX idx_dept_st_dept ON dept_stocktaking(dept_id);
CREATE INDEX idx_dept_st_status ON dept_stocktaking(status);
CREATE INDEX idx_auto_replenish_dept ON auto_replenishment_log(dept_id);

-- 演示数据：基于V17定数配置，为4个科室初始化二级库存（初始量=定数量）
INSERT INTO dept_inventory (dept_id, material_id, current_quantity)
SELECT dept_id, material_id, par_quantity
FROM dept_par_levels
WHERE is_active = true;
