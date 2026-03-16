-- 异常工单表
CREATE TABLE anomaly_work_orders (
    id BIGSERIAL PRIMARY KEY,
    dept_id BIGINT NOT NULL REFERENCES departments(id),
    material_id BIGINT NOT NULL REFERENCES materials(id),
    anomaly_type VARCHAR(20) NOT NULL,
    deviation_rate DOUBLE PRECISION,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    assigned_to BIGINT REFERENCES users(id),
    resolution TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE INDEX idx_anomaly_wo_dept_status ON anomaly_work_orders(dept_id, status);
CREATE INDEX idx_anomaly_wo_status ON anomaly_work_orders(status);
CREATE INDEX idx_anomaly_wo_assigned ON anomaly_work_orders(assigned_to);

-- 异常工单评论表
CREATE TABLE anomaly_work_order_comments (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES anomaly_work_orders(id),
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_anomaly_wo_comments_order ON anomaly_work_order_comments(work_order_id);
