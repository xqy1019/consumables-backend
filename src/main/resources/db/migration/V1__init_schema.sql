-- 科室表
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    dept_code VARCHAR(50) UNIQUE NOT NULL,
    parent_id BIGINT REFERENCES departments(id),
    level INTEGER DEFAULT 1,
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 供应商表
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    supplier_name VARCHAR(200) NOT NULL,
    supplier_code VARCHAR(50) UNIQUE NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(500),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 耗材字典表
CREATE TABLE IF NOT EXISTS materials (
    id BIGSERIAL PRIMARY KEY,
    material_code VARCHAR(50) UNIQUE NOT NULL,
    material_name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    specification VARCHAR(200),
    unit VARCHAR(20),
    supplier_id BIGINT REFERENCES suppliers(id),
    standard_price DECIMAL(10,2),
    min_stock INTEGER DEFAULT 0,
    max_stock INTEGER DEFAULT 1000,
    lead_time INTEGER DEFAULT 7,
    description TEXT,
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 库存表
CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    batch_number VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    location VARCHAR(100),
    manufacture_date DATE,
    expiry_date DATE,
    supplier_id BIGINT REFERENCES suppliers(id),
    receive_date DATE,
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    dept_id BIGINT REFERENCES departments(id),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 申领单表
CREATE TABLE IF NOT EXISTS requisitions (
    id BIGSERIAL PRIMARY KEY,
    requisition_no VARCHAR(50) UNIQUE NOT NULL,
    dept_id BIGINT NOT NULL REFERENCES departments(id),
    requisition_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    required_date DATE,
    status VARCHAR(20) DEFAULT 'DRAFT',
    remark TEXT,
    created_by BIGINT REFERENCES users(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 申领单明细表
CREATE TABLE IF NOT EXISTS requisition_items (
    id BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL REFERENCES requisitions(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    quantity INTEGER NOT NULL,
    actual_quantity INTEGER,
    remark VARCHAR(500)
);

-- 审批记录表
CREATE TABLE IF NOT EXISTS approval_records (
    id BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL REFERENCES requisitions(id),
    approver_id BIGINT REFERENCES users(id),
    approval_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    remark TEXT
);

-- 库存流水记录表
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    transaction_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    batch_number VARCHAR(100),
    dept_id BIGINT REFERENCES departments(id),
    requisition_id BIGINT REFERENCES requisitions(id),
    operator_id BIGINT REFERENCES users(id),
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
