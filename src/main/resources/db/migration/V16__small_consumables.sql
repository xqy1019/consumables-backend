-- V16: 小耗材精细化管理
-- 科室定数管理 + 诊疗消耗包 + 操作记录

-- 1. 科室耗材定数配置
CREATE TABLE dept_par_levels (
    id                BIGSERIAL PRIMARY KEY,
    dept_id           BIGINT        NOT NULL REFERENCES departments(id),
    material_id       BIGINT        NOT NULL REFERENCES materials(id),
    par_quantity      DECIMAL(10,2) NOT NULL DEFAULT 0,  -- 定数（标准库存量）
    min_quantity      DECIMAL(10,2) NOT NULL DEFAULT 0,  -- 最低库存（触发补货预警）
    monthly_limit     DECIMAL(10,2),                     -- 月度领用限额（为空表示不限）
    is_active         BOOLEAN       DEFAULT TRUE,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (dept_id, material_id)
);

-- 2. 诊疗消耗包模板
CREATE TABLE procedure_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(50),          -- 护理操作 / 手术辅助 / 检查操作
    description TEXT,
    is_active   BOOLEAN   DEFAULT TRUE,
    created_by  BIGINT REFERENCES users(id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 消耗包明细
CREATE TABLE procedure_template_items (
    id          BIGSERIAL PRIMARY KEY,
    template_id BIGINT        NOT NULL REFERENCES procedure_templates(id) ON DELETE CASCADE,
    material_id BIGINT        NOT NULL REFERENCES materials(id),
    quantity    DECIMAL(10,2) NOT NULL,
    note        VARCHAR(200)
);

-- 4. 诊疗操作记录（用于倒推小耗材实际消耗）
CREATE TABLE procedure_records (
    id           BIGSERIAL PRIMARY KEY,
    dept_id      BIGINT    NOT NULL REFERENCES departments(id),
    template_id  BIGINT    NOT NULL REFERENCES procedure_templates(id),
    performed_by BIGINT REFERENCES users(id),
    performed_at TIMESTAMP NOT NULL,
    quantity     INTEGER   DEFAULT 1,   -- 本次执行操作次数
    patient_info VARCHAR(100),          -- 病人信息（选填）
    note         TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化常用诊疗消耗包
INSERT INTO procedure_templates (name, category, description) VALUES
('静脉注射',   '护理操作', '常规静脉注射操作标准耗材包'),
('肌肉注射',   '护理操作', '肌肉注射操作标准耗材包'),
('换药（小）', '护理操作', '普通伤口换药耗材包（小伤口）'),
('换药（大）', '护理操作', '普通伤口换药耗材包（大伤口）'),
('留置导尿',   '护理操作', '留置导尿操作标准耗材包'),
('静脉输液',   '护理操作', '静脉输液操作标准耗材包');
