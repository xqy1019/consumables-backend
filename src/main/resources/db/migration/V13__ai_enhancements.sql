-- AI增强功能数据库变更

-- 1. 预测准确率字段
ALTER TABLE ai_prediction_result ADD COLUMN IF NOT EXISTS accuracy_rate DECIMAL(5,2);
ALTER TABLE ai_prediction_result ADD COLUMN IF NOT EXISTS evaluated_at TIMESTAMP;

-- 2. 临期处置建议缓存表（供通知系统读取 AI 建议）
CREATE TABLE IF NOT EXISTS ai_expiry_disposal_cache (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL REFERENCES inventory(id),
    material_id  BIGINT NOT NULL REFERENCES materials(id),
    action       VARCHAR(20) NOT NULL,   -- ACCELERATE/TRANSFER/RETURN/DAMAGE
    reason       VARCHAR(100),
    days_left    INTEGER,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(inventory_id)
);
