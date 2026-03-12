-- 插入科室数据
INSERT INTO departments (dept_name, dept_code, level, description) VALUES
('医院本部', 'HOSPITAL', 1, '医院顶级节点'),
('内科', 'NEIKE', 2, '内科科室'),
('外科', 'WAIKE', 2, '外科科室'),
('急诊科', 'JIZHEN', 2, '急诊科室'),
('中心库房', 'WAREHOUSE', 2, '中心库房') ON CONFLICT DO NOTHING;

UPDATE departments SET parent_id = (SELECT id FROM departments WHERE dept_code = 'HOSPITAL')
WHERE dept_code IN ('NEIKE', 'WAIKE', 'JIZHEN', 'WAREHOUSE');

-- 插入默认角色
INSERT INTO roles (role_name, role_code, description) VALUES
('系统管理员', 'ADMIN', '拥有所有权限的管理员'),
('科室主任', 'DEPT_DIRECTOR', '负责审批科室申领单'),
('护士长', 'HEAD_NURSE', '负责日常申领管理'),
('库管员', 'WAREHOUSE_KEEPER', '负责库存管理'),
('采购员', 'PURCHASER', '负责采购管理'),
('财务人员', 'FINANCE', '负责成本分析') ON CONFLICT DO NOTHING;

-- 插入默认供应商
INSERT INTO suppliers (supplier_name, supplier_code, contact_person, phone, email, address) VALUES
('医疗器械供应商A', 'SUP001', '张三', '13800138000', 'supa@medical.com', '北京市朝阳区医疗路1号'),
('医疗器械供应商B', 'SUP002', '李四', '13900139000', 'supb@medical.com', '上海市浦东新区医疗路2号') ON CONFLICT DO NOTHING;

-- 插入默认管理员用户 (密码: Admin@123456 BCrypt加密)
INSERT INTO users (username, password, real_name, email, dept_id)
SELECT 'admin', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '系统管理员', 'admin@medical.com', d.id
FROM departments d WHERE d.dept_code = 'HOSPITAL' ON CONFLICT DO NOTHING;

-- 给管理员分配管理员角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.role_code = 'ADMIN' ON CONFLICT DO NOTHING;

-- 插入测试用户 (密码: Admin@123456)
INSERT INTO users (username, password, real_name, email, dept_id)
SELECT 'nurse1', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '护士长王五', 'nurse1@medical.com', d.id
FROM departments d WHERE d.dept_code = 'NEIKE' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, dept_id)
SELECT 'keeper1', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '库管员赵六', 'keeper1@medical.com', d.id
FROM departments d WHERE d.dept_code = 'WAREHOUSE' ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'nurse1' AND r.role_code = 'HEAD_NURSE' ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'keeper1' AND r.role_code = 'WAREHOUSE_KEEPER' ON CONFLICT DO NOTHING;

-- 插入示例耗材
INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT001', '一次性注射器', '注射类', '5ml', '支', s.id, 0.50, 100, 5000, 3
FROM suppliers s WHERE s.supplier_code = 'SUP001' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT002', '医用手套', '防护类', 'M号', '双', s.id, 1.20, 200, 10000, 3
FROM suppliers s WHERE s.supplier_code = 'SUP001' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT003', '医用纱布', '敷料类', '10cmx10cm', '块', s.id, 0.30, 500, 20000, 5
FROM suppliers s WHERE s.supplier_code = 'SUP002' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT004', '输液器', '输液类', '普通型', '套', s.id, 2.50, 100, 3000, 3
FROM suppliers s WHERE s.supplier_code = 'SUP002' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT005', '血压计袖带', '检测类', '成人标准型', '个', s.id, 15.00, 10, 50, 7
FROM suppliers s WHERE s.supplier_code = 'SUP001' ON CONFLICT DO NOTHING;

-- 插入示例库存
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id, 'BATCH2024001', 500, 'A区-01', '2024-01-01', '2026-12-31', s.id, '2024-03-01', 1
FROM materials m, suppliers s WHERE m.material_code = 'MAT001' AND s.supplier_code = 'SUP001';

INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id, 'BATCH2024002', 1000, 'A区-02', '2024-01-15', '2026-06-30', s.id, '2024-03-01', 1
FROM materials m, suppliers s WHERE m.material_code = 'MAT002' AND s.supplier_code = 'SUP001';

INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id, 'BATCH2024003', 80, 'B区-01', '2024-02-01', '2026-08-30', s.id, '2024-03-01', 1
FROM materials m, suppliers s WHERE m.material_code = 'MAT003' AND s.supplier_code = 'SUP002';

INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id, 'BATCH2024004', 200, 'B区-02', '2024-02-15', '2026-08-31', s.id, '2024-03-01', 1
FROM materials m, suppliers s WHERE m.material_code = 'MAT004' AND s.supplier_code = 'SUP002';
