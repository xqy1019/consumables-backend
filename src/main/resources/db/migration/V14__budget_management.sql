-- V14: 科室预算管理模块

-- 预算计划表
CREATE TABLE IF NOT EXISTS budget_plans (
    id          BIGSERIAL PRIMARY KEY,
    dept_id     BIGINT NOT NULL REFERENCES departments(id),
    year        INTEGER NOT NULL,
    quarter     INTEGER,                          -- NULL 表示年度预算，1-4 表示季度
    category    VARCHAR(100),                     -- 耗材分类（NULL表示不限分类）
    budget_amount NUMERIC(12,2) NOT NULL,         -- 预算金额
    used_amount   NUMERIC(12,2) DEFAULT 0,        -- 已使用金额
    status      VARCHAR(20) DEFAULT 'ACTIVE',     -- ACTIVE / CLOSED
    remark      VARCHAR(500),
    created_by  BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (dept_id, year, quarter, category)
);

-- 预算执行记录表（记录每次申领消耗预算的明细）
CREATE TABLE IF NOT EXISTS budget_executions (
    id              BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT NOT NULL REFERENCES budget_plans(id),
    requisition_id  BIGINT REFERENCES requisitions(id),
    dept_id         BIGINT NOT NULL REFERENCES departments(id),
    amount          NUMERIC(12,2) NOT NULL,
    description     VARCHAR(500),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_budget_plans_dept_year ON budget_plans(dept_id, year);
CREATE INDEX idx_budget_executions_plan ON budget_executions(plan_id);

-- 初始化几个演示预算
INSERT INTO budget_plans (dept_id, year, quarter, category, budget_amount, used_amount, remark, created_by)
SELECT d.id, 2026, NULL, NULL, 500000.00, 0, '2026年度综合预算', (SELECT id FROM users WHERE username='admin' LIMIT 1)
FROM departments d WHERE d.dept_name IN ('心脏外科', '神经外科', '骨科') AND d.status = 1
ON CONFLICT DO NOTHING;
