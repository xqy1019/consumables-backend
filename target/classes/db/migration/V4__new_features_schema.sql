-- =====================================================
-- V4: 新增功能表结构
-- 字典管理、库存扩展、高值追溯、AI预测、采购管理
-- =====================================================

-- =====================================================
-- 1. 字典管理
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGSERIAL PRIMARY KEY,
    dict_name VARCHAR(100) NOT NULL,
    dict_code VARCHAR(50) UNIQUE NOT NULL,
    dict_type VARCHAR(20) DEFAULT 'normal',
    remark VARCHAR(500),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGSERIAL PRIMARY KEY,
    dict_id BIGINT NOT NULL REFERENCES sys_dict(id) ON DELETE CASCADE,
    item_label VARCHAR(100) NOT NULL,
    item_value VARCHAR(100) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    remark VARCHAR(500),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. 盘点管理
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_stocktaking (
    id BIGSERIAL PRIMARY KEY,
    stocktaking_no VARCHAR(50) UNIQUE NOT NULL,
    stocktaking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_stocktaking_item (
    id BIGSERIAL PRIMARY KEY,
    stocktaking_id BIGINT NOT NULL REFERENCES inventory_stocktaking(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    inventory_id BIGINT REFERENCES inventory(id),
    batch_number VARCHAR(100),
    system_quantity INTEGER DEFAULT 0,
    actual_quantity INTEGER,
    difference INTEGER,
    remark VARCHAR(500)
);

-- =====================================================
-- 3. 移库管理
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_transfer (
    id BIGSERIAL PRIMARY KEY,
    transfer_no VARCHAR(50) UNIQUE NOT NULL,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    inventory_id BIGINT NOT NULL REFERENCES inventory(id),
    quantity INTEGER NOT NULL,
    from_location VARCHAR(100),
    to_location VARCHAR(100) NOT NULL,
    transfer_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    remark VARCHAR(500),
    operator_id BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 4. 报损管理
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_damage (
    id BIGSERIAL PRIMARY KEY,
    damage_no VARCHAR(50) UNIQUE NOT NULL,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    inventory_id BIGINT NOT NULL REFERENCES inventory(id),
    batch_number VARCHAR(100),
    quantity INTEGER NOT NULL,
    damage_reason VARCHAR(200) NOT NULL,
    damage_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    remark TEXT,
    operator_id BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 5. 借用管理
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_borrowing (
    id BIGSERIAL PRIMARY KEY,
    borrowing_no VARCHAR(50) UNIQUE NOT NULL,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    inventory_id BIGINT NOT NULL REFERENCES inventory(id),
    batch_number VARCHAR(100),
    quantity INTEGER NOT NULL,
    dept_id BIGINT REFERENCES departments(id),
    borrower_name VARCHAR(100),
    borrowing_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_return_date DATE,
    actual_return_date DATE,
    status VARCHAR(20) DEFAULT 'BORROWED',
    remark TEXT,
    operator_id BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 6. 高值耗材 UDI 追溯
-- =====================================================
CREATE TABLE IF NOT EXISTS material_udi (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    inventory_id BIGINT REFERENCES inventory(id),
    udi_code VARCHAR(100) UNIQUE NOT NULL,
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    manufacture_date DATE,
    expiry_date DATE,
    supplier_id BIGINT REFERENCES suppliers(id),
    status VARCHAR(20) DEFAULT 'IN_STOCK',
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 7. 手术记录
-- =====================================================
CREATE TABLE IF NOT EXISTS surgery_record (
    id BIGSERIAL PRIMARY KEY,
    surgery_no VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50),
    patient_name VARCHAR(100) NOT NULL,
    patient_age INTEGER,
    patient_gender VARCHAR(10),
    dept_id BIGINT REFERENCES departments(id),
    surgery_date TIMESTAMP NOT NULL,
    surgery_type VARCHAR(200),
    doctor_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS surgery_material_binding (
    id BIGSERIAL PRIMARY KEY,
    surgery_id BIGINT NOT NULL REFERENCES surgery_record(id) ON DELETE CASCADE,
    udi_id BIGINT REFERENCES material_udi(id),
    material_id BIGINT NOT NULL REFERENCES materials(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    use_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
);

-- =====================================================
-- 8. AI 预测结果
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_prediction_result (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    dept_id BIGINT REFERENCES departments(id),
    prediction_month VARCHAR(7) NOT NULL,
    predicted_quantity INTEGER NOT NULL,
    actual_quantity INTEGER,
    confidence DECIMAL(5,2),
    algorithm VARCHAR(50) DEFAULT 'MA',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(material_id, dept_id, prediction_month)
);

-- =====================================================
-- 9. 采购请购单
-- =====================================================
CREATE TABLE IF NOT EXISTS purchase_requisition (
    id BIGSERIAL PRIMARY KEY,
    req_no VARCHAR(50) UNIQUE NOT NULL,
    dept_id BIGINT REFERENCES departments(id),
    req_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    required_date DATE,
    status VARCHAR(20) DEFAULT 'DRAFT',
    total_amount DECIMAL(12,2) DEFAULT 0,
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    approved_by BIGINT REFERENCES users(id),
    approved_time TIMESTAMP,
    approval_remark TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_requisition_item (
    id BIGSERIAL PRIMARY KEY,
    req_id BIGINT NOT NULL REFERENCES purchase_requisition(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    quantity INTEGER NOT NULL,
    estimated_price DECIMAL(10,2),
    subtotal DECIMAL(12,2),
    remark VARCHAR(500)
);

-- =====================================================
-- 10. 询价单
-- =====================================================
CREATE TABLE IF NOT EXISTS purchase_inquiry (
    id BIGSERIAL PRIMARY KEY,
    inquiry_no VARCHAR(50) UNIQUE NOT NULL,
    req_id BIGINT REFERENCES purchase_requisition(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    inquiry_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_date DATE,
    status VARCHAR(20) DEFAULT 'SENT',
    total_amount DECIMAL(12,2) DEFAULT 0,
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_inquiry_item (
    id BIGSERIAL PRIMARY KEY,
    inquiry_id BIGINT NOT NULL REFERENCES purchase_inquiry(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    quantity INTEGER NOT NULL,
    quoted_price DECIMAL(10,2),
    subtotal DECIMAL(12,2),
    delivery_days INTEGER,
    remark VARCHAR(500)
);

-- =====================================================
-- 11. 采购合同
-- =====================================================
CREATE TABLE IF NOT EXISTS purchase_contract (
    id BIGSERIAL PRIMARY KEY,
    contract_no VARCHAR(50) UNIQUE NOT NULL,
    inquiry_id BIGINT REFERENCES purchase_inquiry(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    contract_date DATE NOT NULL,
    delivery_date DATE,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_contract_item (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES purchase_contract(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    delivered_quantity INTEGER DEFAULT 0,
    remark VARCHAR(500)
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_sys_dict_item_dict_id ON sys_dict_item(dict_id);
CREATE INDEX IF NOT EXISTS idx_material_udi_material_id ON material_udi(material_id);
CREATE INDEX IF NOT EXISTS idx_material_udi_udi_code ON material_udi(udi_code);
CREATE INDEX IF NOT EXISTS idx_surgery_material_surgery_id ON surgery_material_binding(surgery_id);
CREATE INDEX IF NOT EXISTS idx_ai_prediction_material_id ON ai_prediction_result(material_id);
CREATE INDEX IF NOT EXISTS idx_purchase_req_status ON purchase_requisition(status);
CREATE INDEX IF NOT EXISTS idx_inventory_stocktaking_status ON inventory_stocktaking(status);
