-- V15: 供应商绩效评价体系

-- 供应商绩效评价记录表
CREATE TABLE IF NOT EXISTS supplier_evaluations (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT NOT NULL REFERENCES suppliers(id),
    eval_year       INTEGER NOT NULL,
    eval_quarter    INTEGER NOT NULL,              -- 1-4
    -- 各维度得分（0-100）
    price_score     NUMERIC(5,2) DEFAULT 0,        -- 价格竞争力得分（25%）
    quality_score   NUMERIC(5,2) DEFAULT 0,        -- 质量合格率得分（40%）
    delivery_score  NUMERIC(5,2) DEFAULT 0,        -- 交货及时率得分（25%）
    service_score   NUMERIC(5,2) DEFAULT 0,        -- 综合服务得分（10%）
    total_score     NUMERIC(5,2) DEFAULT 0,        -- 综合得分（加权）
    grade           VARCHAR(10),                   -- 等级：A/B/C/D
    -- 原始数据
    quality_rate    NUMERIC(5,2),                  -- 质量合格率 %
    delivery_rate   NUMERIC(5,2),                  -- 交货及时率 %
    avg_price_ratio NUMERIC(6,4),                  -- 平均价格比（实际/市场均价，<1 越好）
    -- 说明
    remark          TEXT,
    evaluated_by    BIGINT REFERENCES users(id),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_supplier_eval_uniq ON supplier_evaluations(supplier_id, eval_year, eval_quarter);
CREATE INDEX idx_supplier_eval_supplier ON supplier_evaluations(supplier_id);

-- 采购合同增加到货情况字段（若尚未有）
ALTER TABLE purchase_contract ADD COLUMN IF NOT EXISTS expected_delivery_date DATE;
ALTER TABLE purchase_contract ADD COLUMN IF NOT EXISTS actual_delivery_date DATE;
