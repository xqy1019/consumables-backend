-- =====================================================
-- V7: 医疗器械注册证、召回管理、入库验收、申领签收、退料申请
-- =====================================================

-- =====================================================
-- 1. 耗材注册证字段
-- =====================================================
ALTER TABLE materials
    ADD COLUMN IF NOT EXISTS registration_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS registration_expiry DATE,
    ADD COLUMN IF NOT EXISTS manufacturer VARCHAR(200),
    ADD COLUMN IF NOT EXISTS is_high_value BOOLEAN DEFAULT FALSE;

-- =====================================================
-- 2. 供应商经营许可证字段
-- =====================================================
ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS license_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS license_expiry DATE;

-- =====================================================
-- 3. 召回通知主表
-- =====================================================
CREATE TABLE IF NOT EXISTS recall_notices (
    id BIGSERIAL PRIMARY KEY,
    recall_no VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    recall_reason TEXT,
    recall_level VARCHAR(10) DEFAULT 'II',   -- I最严重 / II中等 / III轻微
    source VARCHAR(20) DEFAULT 'SUPPLIER',   -- SUPPLIER / REGULATOR
    issued_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',     -- ACTIVE / CLOSED
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 4. 召回受影响批次
-- =====================================================
CREATE TABLE IF NOT EXISTS recall_notice_batches (
    id BIGSERIAL PRIMARY KEY,
    recall_id BIGINT NOT NULL REFERENCES recall_notices(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    batch_number VARCHAR(100),   -- NULL 表示该耗材所有批次
    quantity_affected INTEGER,
    remark VARCHAR(500)
);

-- =====================================================
-- 5. 召回处置记录
-- =====================================================
CREATE TABLE IF NOT EXISTS recall_disposals (
    id BIGSERIAL PRIMARY KEY,
    recall_id BIGINT NOT NULL REFERENCES recall_notices(id),
    inventory_id BIGINT REFERENCES inventory(id),
    material_id BIGINT NOT NULL REFERENCES materials(id),
    batch_number VARCHAR(100),
    quantity INTEGER NOT NULL,
    disposal_type VARCHAR(20) NOT NULL,  -- RETURN / DESTROY / QUARANTINE
    disposal_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark TEXT,
    operator_id BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 6. 库存入库验收字段
-- =====================================================
ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS inspection_status VARCHAR(20) DEFAULT 'PASSED',
    ADD COLUMN IF NOT EXISTS inspection_remark VARCHAR(500),
    ADD COLUMN IF NOT EXISTS inspector_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS inspect_time TIMESTAMP;

-- =====================================================
-- 7. 申领单签收字段
-- =====================================================
ALTER TABLE requisitions
    ADD COLUMN IF NOT EXISTS signed_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS sign_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sign_remark TEXT;

-- =====================================================
-- 8. 退料申请主表
-- =====================================================
CREATE TABLE IF NOT EXISTS return_requests (
    id BIGSERIAL PRIMARY KEY,
    return_no VARCHAR(50) UNIQUE NOT NULL,
    dept_id BIGINT REFERENCES departments(id),
    status VARCHAR(20) DEFAULT 'PENDING',   -- PENDING / APPROVED / COMPLETED / REJECTED
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    approved_by BIGINT REFERENCES users(id),
    approved_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 9. 退料申请明细
-- =====================================================
CREATE TABLE IF NOT EXISTS return_request_items (
    id BIGSERIAL PRIMARY KEY,
    return_id BIGINT NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    batch_number VARCHAR(100),
    quantity INTEGER NOT NULL,
    remark VARCHAR(500)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_recall_notices_status ON recall_notices(status);
CREATE INDEX IF NOT EXISTS idx_recall_batches_recall_id ON recall_notice_batches(recall_id);
CREATE INDEX IF NOT EXISTS idx_recall_batches_material_id ON recall_notice_batches(material_id);
CREATE INDEX IF NOT EXISTS idx_recall_disposals_recall_id ON recall_disposals(recall_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_dept_id ON return_requests(dept_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_status ON return_requests(status);
CREATE INDEX IF NOT EXISTS idx_inventory_inspection_status ON inventory(inspection_status);
