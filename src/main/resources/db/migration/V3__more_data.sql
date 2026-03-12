-- =====================================================
-- V3: 丰富系统演示数据
-- =====================================================

-- =====================================================
-- 1. 新增科室 (8个)
-- =====================================================
INSERT INTO departments (dept_name, dept_code, level, description) VALUES
('骨科',        'GUTKE',   2, '骨科及骨外科'),
('妇产科',      'FUCHANK', 2, '妇科及产科'),
('儿科',        'ERKE',    2, '儿科门诊及住院'),
('心内科',      'XINNEIKE',2, '心脏内科'),
('ICU重症监护', 'ICU',     2, '重症监护室'),
('手术室',      'SHOUSHU', 2, '综合手术室'),
('检验科',      'JIANYAN', 2, '医学检验科'),
('放射科',      'FANGSHE', 2, '放射影像科')
ON CONFLICT DO NOTHING;

UPDATE departments
SET parent_id = (SELECT id FROM departments WHERE dept_code = 'HOSPITAL')
WHERE dept_code IN ('GUTKE','FUCHANK','ERKE','XINNEIKE','ICU','SHOUSHU','JIANYAN','FANGSHE')
  AND parent_id IS NULL;

-- =====================================================
-- 2. 新增供应商 (3个)
-- =====================================================
INSERT INTO suppliers (supplier_name, supplier_code, contact_person, phone, email, address) VALUES
('恒瑞医疗科技有限公司', 'SUP003', '王志远', '13700137000', 'wzy@hengrui.com',  '江苏省连云港市海州区医疗路88号'),
('百洋医疗器械股份公司', 'SUP004', '陈明华', '13600136000', 'cmh@baiyang.com', '广东省广州市天河区科技路36号'),
('康复之家医疗器械公司', 'SUP005', '刘晓芳', '13500135000', 'lxf@kangfu.com',  '北京市海淀区中关村北路12号')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 3. 新增耗材 (20种，涵盖各分类)
-- =====================================================
INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT006', '医用外科口罩',   '防护类', '三层无纺布防护',  '只', id, 0.80, 500, 20000, 3 FROM suppliers WHERE supplier_code='SUP001' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT007', '一次性导尿管',   '导管类', '普通型16Fr硅胶', '根', id, 12.50, 50, 500, 5 FROM suppliers WHERE supplier_code='SUP003' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT008', '心电监护导联线', '监测类', '5导联标准接口', '套', id, 45.00, 20, 100, 14 FROM suppliers WHERE supplier_code='SUP004' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT009', '医用棉球',       '消毒类', '中号100粒/袋',   '袋', id, 2.80, 200, 5000, 3 FROM suppliers WHERE supplier_code='SUP001' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT010', '外科无菌手套',   '防护类', 'M号无粉乳胶',    '双', id, 3.50, 300, 10000, 3 FROM suppliers WHERE supplier_code='SUP002' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT011', '一次性手术帽',   '防护类', '弹力无纺布',     '只', id, 0.60, 500, 20000, 3 FROM suppliers WHERE supplier_code='SUP001' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT012', '止血纱布',       '敷料类', '10cmx20cm无菌', '块', id, 1.20, 200, 5000, 3 FROM suppliers WHERE supplier_code='SUP002' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT013', '静脉留置针',     '注射类', '24G蓝色翼型',   '支', id, 4.80, 100, 3000, 5 FROM suppliers WHERE supplier_code='SUP003' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT014', '真空采血管',     '检验类', '负压5ml紫盖EDTA','支', id, 1.50, 300, 10000, 3 FROM suppliers WHERE supplier_code='SUP004' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT015', '医用酒精棉片',   '消毒类', '75%酒精独立包装','片', id, 0.15, 1000, 50000, 3 FROM suppliers WHERE supplier_code='SUP001' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT016', '一次性引流袋',   '导管类', '2000ml防逆流',  '个', id, 8.50, 50, 500, 5 FROM suppliers WHERE supplier_code='SUP003' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT017', '骨科固定绷带',   '骨科类', '8cm×4.5m高弹',  '卷', id, 5.50, 100, 2000, 7 FROM suppliers WHERE supplier_code='SUP005' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT018', '除颤仪电极片',   '急救类', '成人通用自粘',  '对', id, 85.00, 10, 50, 14 FROM suppliers WHERE supplier_code='SUP004' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT019', '负压引流球',     '外科类', '100ml JP式',    '个', id, 15.00, 30, 300, 7 FROM suppliers WHERE supplier_code='SUP003' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT020', '医用弹力袜',     '防护类', '中筒一级压力',  '双', id, 35.00, 20, 200, 14 FROM suppliers WHERE supplier_code='SUP005' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT021', '雾化吸入面罩',   '呼吸类', '成人带管路组合','套', id, 12.00, 30, 200, 7 FROM suppliers WHERE supplier_code='SUP003' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT022', '血糖检测试纸',   '检验类', '50片/盒通用型', '盒', id, 28.00, 200, 2000, 7 FROM suppliers WHERE supplier_code='SUP004' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT023', '胎心监护耦合剂', '产科类', '250g装无味型',  '瓶', id, 18.00, 20, 200, 7 FROM suppliers WHERE supplier_code='SUP005' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT024', '心电图电极片',   '监测类', '成人50片/盒',   '盒', id, 25.00, 50, 500, 7 FROM suppliers WHERE supplier_code='SUP004' ON CONFLICT DO NOTHING;

INSERT INTO materials (material_code, material_name, category, specification, unit, supplier_id, standard_price, min_stock, max_stock, lead_time)
SELECT 'MAT025', '内镜活检钳',     '内镜类', '2.2mm标准可重复','把', id, 120.00, 10, 100, 14 FROM suppliers WHERE supplier_code='SUP003' ON CONFLICT DO NOTHING;

-- =====================================================
-- 4. 新增用户 (7个，涵盖所有角色)
-- =====================================================
INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'director1', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '内科主任李明', 'director1@medical.com', '13811111111', d.id
FROM departments d WHERE d.dept_code = 'NEIKE' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'director2', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '外科主任张勇', 'director2@medical.com', '13822222222', d.id
FROM departments d WHERE d.dept_code = 'WAIKE' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'nurse2', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '外科护士长周颖', 'nurse2@medical.com', '13833333333', d.id
FROM departments d WHERE d.dept_code = 'WAIKE' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'nurse3', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '急诊护士长陈丽', 'nurse3@medical.com', '13844444444', d.id
FROM departments d WHERE d.dept_code = 'JIZHEN' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'keeper2', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '库管员刘强', 'keeper2@medical.com', '13855555555', d.id
FROM departments d WHERE d.dept_code = 'WAREHOUSE' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'purchaser1', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '采购员孙伟', 'purchaser1@medical.com', '13866666666', d.id
FROM departments d WHERE d.dept_code = 'WAREHOUSE' ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, real_name, email, phone, dept_id)
SELECT 'finance1', '$2a$10$KxiU4/zi3uc0ya9pHW.P7uweWwuxrjOy48GL7u6LA667PoDje2v2.', '财务人员吴雪', 'finance1@medical.com', '13877777777', d.id
FROM departments d WHERE d.dept_code = 'HOSPITAL' ON CONFLICT DO NOTHING;

-- 分配角色
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='director1'  AND r.role_code='DEPT_DIRECTOR'     ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='director2'  AND r.role_code='DEPT_DIRECTOR'     ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='nurse2'     AND r.role_code='HEAD_NURSE'        ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='nurse3'     AND r.role_code='HEAD_NURSE'        ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='keeper2'    AND r.role_code='WAREHOUSE_KEEPER'  ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='purchaser1' AND r.role_code='PURCHASER'         ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='finance1'   AND r.role_code='FINANCE'           ON CONFLICT DO NOTHING;

-- =====================================================
-- 5. 库存批次 (含正常/低库存/即将过期)
-- =====================================================
-- 防护类 - 医用外科口罩 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M06A', 3000,'A区-03','2025-06-01','2027-05-31',s.id,'2025-06-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT006' AND s.supplier_code='SUP001';

-- 防护类 - 医用外科口罩 (即将过期 2026-03-20)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2024M06B', 200,'A区-03','2024-03-15','2026-03-20',s.id,'2024-03-20',1
FROM materials m, suppliers s WHERE m.material_code='MAT006' AND s.supplier_code='SUP001';

-- 导管类 - 导尿管 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M07A', 150,'B区-03','2025-08-01','2028-07-31',s.id,'2025-08-15',1
FROM materials m, suppliers s WHERE m.material_code='MAT007' AND s.supplier_code='SUP003';

-- 监测类 - 心电导联线 (低库存: 15 < min_stock 20)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M08A', 15,'C区-01','2025-01-01','2028-12-31',s.id,'2025-01-20',1
FROM materials m, suppliers s WHERE m.material_code='MAT008' AND s.supplier_code='SUP004';

-- 消毒类 - 医用棉球 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M09A', 800,'A区-04','2025-05-01','2027-04-30',s.id,'2025-05-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT009' AND s.supplier_code='SUP001';

-- 防护类 - 外科手套 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M10A', 2000,'A区-05','2025-07-01','2028-06-30',s.id,'2025-07-15',1
FROM materials m, suppliers s WHERE m.material_code='MAT010' AND s.supplier_code='SUP002';

-- 防护类 - 手术帽 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M11A', 5000,'A区-06','2025-09-01','2028-08-31',s.id,'2025-09-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT011' AND s.supplier_code='SUP001';

-- 敷料类 - 止血纱布 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M12A', 1500,'B区-04','2025-04-01','2027-03-31',s.id,'2025-04-15',1
FROM materials m, suppliers s WHERE m.material_code='MAT012' AND s.supplier_code='SUP002';

-- 注射类 - 静脉留置针 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M13A', 500,'A区-07','2025-10-01','2028-09-30',s.id,'2025-10-20',1
FROM materials m, suppliers s WHERE m.material_code='MAT013' AND s.supplier_code='SUP003';

-- 检验类 - 真空采血管 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M14A', 2000,'C区-02','2025-11-01','2027-10-31',s.id,'2025-11-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT014' AND s.supplier_code='SUP004';

-- 消毒类 - 酒精棉片 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M15A', 8000,'A区-08','2025-08-01','2027-07-31',s.id,'2025-08-20',1
FROM materials m, suppliers s WHERE m.material_code='MAT015' AND s.supplier_code='SUP001';

-- 导管类 - 引流袋 (即将过期 2026-03-25)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2024M16A', 80,'B区-05','2024-03-20','2026-03-25',s.id,'2024-03-25',1
FROM materials m, suppliers s WHERE m.material_code='MAT016' AND s.supplier_code='SUP003';

-- 骨科类 - 骨科绷带 (低库存: 25 < min_stock 100)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M17A', 25,'D区-01','2025-06-01','2027-05-31',s.id,'2025-06-15',1
FROM materials m, suppliers s WHERE m.material_code='MAT017' AND s.supplier_code='SUP005';

-- 急救类 - 除颤仪电极片 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M18A', 30,'C区-03','2025-03-01','2028-02-28',s.id,'2025-03-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT018' AND s.supplier_code='SUP004';

-- 外科类 - 负压引流球 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M19A', 120,'B区-06','2025-05-01','2028-04-30',s.id,'2025-05-20',1
FROM materials m, suppliers s WHERE m.material_code='MAT019' AND s.supplier_code='SUP003';

-- 防护类 - 医用弹力袜 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M20A', 60,'D区-02','2025-09-01',NULL,s.id,'2025-09-15',1
FROM materials m, suppliers s WHERE m.material_code='MAT020' AND s.supplier_code='SUP005';

-- 呼吸类 - 雾化面罩 (即将过期 2026-04-01)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2024M21A', 45,'B区-07','2024-04-01','2026-04-01',s.id,'2024-04-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT021' AND s.supplier_code='SUP003';

-- 检验类 - 血糖试纸 (低库存: 80 < min_stock 200)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M22A', 80,'C区-04','2025-10-01','2026-09-30',s.id,'2025-10-15',1
FROM materials m, suppliers s WHERE m.material_code='MAT022' AND s.supplier_code='SUP004';

-- 产科类 - 胎心耦合剂 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M23A', 60,'D区-03','2025-11-01','2027-10-31',s.id,'2025-11-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT023' AND s.supplier_code='SUP005';

-- 监测类 - 心电图电极片 (正常)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M24A', 200,'C区-05','2025-12-01','2027-11-30',s.id,'2025-12-10',1
FROM materials m, suppliers s WHERE m.material_code='MAT024' AND s.supplier_code='SUP004';

-- 内镜类 - 活检钳 (低库存: 4 < min_stock 10)
INSERT INTO inventory (material_id, batch_number, quantity, location, manufacture_date, expiry_date, supplier_id, receive_date, status)
SELECT m.id,'BATCH2025M25A', 4,'E区-01','2025-07-01',NULL,s.id,'2025-07-20',1
FROM materials m, suppliers s WHERE m.material_code='MAT025' AND s.supplier_code='SUP003';

-- =====================================================
-- 6. 申领单 + 明细 + 审批记录
-- =====================================================

-- 申领单1: 草稿 - 内科护士长 (nurse1)
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202601150001', d.id, '2026-01-15 09:00:00', '2026-01-20', 'DRAFT', '内科日常耗材补充', u.id, '2026-01-15 09:00:00'
FROM departments d, users u WHERE d.dept_code='NEIKE' AND u.username='nurse1'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 200, '注射器补充' FROM requisitions r, materials m WHERE r.requisition_no='REQ202601150001' AND m.material_code='MAT001';
INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 100, '手套补充' FROM requisitions r, materials m WHERE r.requisition_no='REQ202601150001' AND m.material_code='MAT010';

-- 申领单2: 草稿 - 外科护士长 (nurse2)
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202601160001', d.id, '2026-01-16 10:30:00', '2026-01-22', 'DRAFT', '外科手术室耗材', u.id, '2026-01-16 10:30:00'
FROM departments d, users u WHERE d.dept_code='WAIKE' AND u.username='nurse2'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 50, '手术用' FROM requisitions r, materials m WHERE r.requisition_no='REQ202601160001' AND m.material_code='MAT019';
INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 100, '手术手套' FROM requisitions r, materials m WHERE r.requisition_no='REQ202601160001' AND m.material_code='MAT010';
INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 100, '手术帽' FROM requisitions r, materials m WHERE r.requisition_no='REQ202601160001' AND m.material_code='MAT011';

-- 申领单3: 待审批 - 内科护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202601200001', d.id, '2026-01-20 08:00:00', '2026-01-25', 'PENDING', '急需棉球和酒精棉片', u.id, '2026-01-20 08:00:00'
FROM departments d, users u WHERE d.dept_code='NEIKE' AND u.username='nurse1'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 50 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601200001' AND m.material_code='MAT009';
INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 500 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601200001' AND m.material_code='MAT015';

-- 申领单4: 待审批 - 急诊护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202601220001', d.id, '2026-01-22 14:00:00', '2026-01-28', 'PENDING', '急诊科急救耗材补充', u.id, '2026-01-22 14:00:00'
FROM departments d, users u WHERE d.dept_code='JIZHEN' AND u.username='nurse3'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 5, '急救备用' FROM requisitions r, materials m WHERE r.requisition_no='REQ202601220001' AND m.material_code='MAT018';
INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 200 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601220001' AND m.material_code='MAT001';
INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 300 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601220001' AND m.material_code='MAT004';

-- 申领单5: 已审批 - 外科护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202601250001', d.id, '2026-01-25 09:00:00', '2026-01-30', 'APPROVED', '外科病房常规耗材', u.id, '2026-01-25 09:00:00'
FROM departments d, users u WHERE d.dept_code='WAIKE' AND u.username='nurse2'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 300 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601250001' AND m.material_code='MAT002';
INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 200 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601250001' AND m.material_code='MAT003';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-01-26 10:00:00', 'APPROVED', '耗材充足，同意发放'
FROM requisitions r, users u WHERE r.requisition_no='REQ202601250001' AND u.username='director2';

-- 申领单6: 已审批 - 内科护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202601280001', d.id, '2026-01-28 11:00:00', '2026-02-05', 'APPROVED', '内科病区月度补充', u.id, '2026-01-28 11:00:00'
FROM departments d, users u WHERE d.dept_code='NEIKE' AND u.username='nurse1'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 500 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601280001' AND m.material_code='MAT001';
INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 100 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601280001' AND m.material_code='MAT004';
INSERT INTO requisition_items (requisition_id, material_id, quantity)
SELECT r.id, m.id, 200 FROM requisitions r, materials m WHERE r.requisition_no='REQ202601280001' AND m.material_code='MAT013';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-01-29 09:30:00', 'APPROVED', '审核通过'
FROM requisitions r, users u WHERE r.requisition_no='REQ202601280001' AND u.username='director1';

-- 申领单7: 已驳回 - 急诊护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202602010001', d.id, '2026-02-01 16:00:00', '2026-02-10', 'REJECTED', '申领数量过多', u.id, '2026-02-01 16:00:00'
FROM departments d, users u WHERE d.dept_code='JIZHEN' AND u.username='nurse3'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, remark)
SELECT r.id, m.id, 5000, '数量过多被驳回' FROM requisitions r, materials m WHERE r.requisition_no='REQ202602010001' AND m.material_code='MAT002';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-02 10:00:00', 'REJECTED', '申领数量超出科室用量，请按实际需求重新申领'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602010001' AND u.username='director1';

-- 申领单8: 已发放 - 内科护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202602100001', d.id, '2026-02-10 09:00:00', '2026-02-15', 'DISPATCHED', '春节后恢复期补充', u.id, '2026-02-10 09:00:00'
FROM departments d, users u WHERE d.dept_code='NEIKE' AND u.username='nurse1'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 300, 300 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602100001' AND m.material_code='MAT001';
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 500, 500 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602100001' AND m.material_code='MAT015';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-11 10:00:00', 'APPROVED', '审核通过'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602100001' AND u.username='director1';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-12 14:00:00', 'DISPATCHED', '已按实际数量发放'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602100001' AND u.username='keeper1';

-- 申领单9: 已发放 - 外科护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202602150001', d.id, '2026-02-15 10:00:00', '2026-02-20', 'DISPATCHED', '外科手术室补充', u.id, '2026-02-15 10:00:00'
FROM departments d, users u WHERE d.dept_code='WAIKE' AND u.username='nurse2'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 200, 200 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602150001' AND m.material_code='MAT010';
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 300, 280 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602150001' AND m.material_code='MAT011';
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 30, 30 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602150001' AND m.material_code='MAT019';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-16 09:30:00', 'APPROVED', '同意'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602150001' AND u.username='director2';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-17 11:00:00', 'DISPATCHED', '手术帽库存不足，实发280个'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602150001' AND u.username='keeper1';

-- 申领单10: 已发放 - 急诊护士长
INSERT INTO requisitions (requisition_no, dept_id, requisition_date, required_date, status, remark, created_by, create_time)
SELECT 'REQ202602220001', d.id, '2026-02-22 08:30:00', '2026-02-25', 'DISPATCHED', '急诊科常规补充', u.id, '2026-02-22 08:30:00'
FROM departments d, users u WHERE d.dept_code='JIZHEN' AND u.username='nurse3'
ON CONFLICT DO NOTHING;

INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 100, 100 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602220001' AND m.material_code='MAT001';
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 200, 200 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602220001' AND m.material_code='MAT004';
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT r.id, m.id, 100, 100 FROM requisitions r, materials m WHERE r.requisition_no='REQ202602220001' AND m.material_code='MAT009';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-23 10:00:00', 'APPROVED', '急诊优先，审核通过'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602220001' AND u.username='director1';

INSERT INTO approval_records (requisition_id, approver_id, approval_time, status, remark)
SELECT r.id, u.id, '2026-02-23 14:00:00', 'DISPATCHED', '已发放完毕'
FROM requisitions r, users u WHERE r.requisition_no='REQ202602220001' AND u.username='keeper2';

-- =====================================================
-- 7. 库存流水记录 (近7天，用于 Dashboard 趋势图)
-- 日期: 2026-02-26 至 2026-03-04
-- =====================================================

-- 2026-02-26 (周四)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',500,'BATCH2025M06A',u.id,'医用口罩入库','2026-02-26 09:00:00'
FROM materials m, users u WHERE m.material_code='MAT006' AND u.username='keeper1';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',200,'BATCH2025M06A',d.id,u.id,'发放内科','2026-02-26 14:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT006' AND u.username='keeper1' AND d.dept_code='NEIKE';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',1000,'BATCH2025M15A',u.id,'酒精棉片入库','2026-02-26 10:00:00'
FROM materials m, users u WHERE m.material_code='MAT015' AND u.username='keeper2';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',100,'BATCH2024001',d.id,u.id,'急诊科领用','2026-02-26 15:30:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT001' AND u.username='keeper1' AND d.dept_code='JIZHEN';

-- 2026-02-27 (周五)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',300,'BATCH2025M10A',u.id,'外科手套入库','2026-02-27 08:30:00'
FROM materials m, users u WHERE m.material_code='MAT010' AND u.username='keeper1';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',150,'BATCH2025M10A',d.id,u.id,'手术室领用','2026-02-27 10:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT010' AND u.username='keeper1' AND d.dept_code='SHOUSHU';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',200,'BATCH2025M09A',u.id,'医用棉球补充','2026-02-27 14:00:00'
FROM materials m, users u WHERE m.material_code='MAT009' AND u.username='keeper2';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',80,'BATCH2025M09A',d.id,u.id,'内科领用','2026-02-27 16:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT009' AND u.username='keeper1' AND d.dept_code='NEIKE';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',50,'BATCH2024002',d.id,u.id,'外科领用手套','2026-02-27 11:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT002' AND u.username='keeper2' AND d.dept_code='WAIKE';

-- 2026-02-28 (周六)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',100,'BATCH2025M13A',u.id,'静脉留置针入库','2026-02-28 09:00:00'
FROM materials m, users u WHERE m.material_code='MAT013' AND u.username='keeper1';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',500,'BATCH2025M14A',u.id,'采血管入库','2026-02-28 10:30:00'
FROM materials m, users u WHERE m.material_code='MAT014' AND u.username='keeper2';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',30,'BATCH2025M13A',d.id,u.id,'ICU领用','2026-02-28 14:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT013' AND u.username='keeper1' AND d.dept_code='ICU';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',200,'BATCH2025M14A',d.id,u.id,'检验科领用','2026-02-28 15:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT014' AND u.username='keeper2' AND d.dept_code='JIANYAN';

-- 2026-03-01 (周日)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',100,'BATCH2024001',d.id,u.id,'内科领用注射器','2026-03-01 09:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT001' AND u.username='keeper1' AND d.dept_code='NEIKE';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',200,'BATCH2025M15A',d.id,u.id,'外科领用棉片','2026-03-01 10:30:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT015' AND u.username='keeper1' AND d.dept_code='WAIKE';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',200,'BATCH2025M12A',u.id,'止血纱布补货','2026-03-01 14:00:00'
FROM materials m, users u WHERE m.material_code='MAT012' AND u.username='keeper2';

-- 2026-03-02 (周一)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',1000,'BATCH2025M11A',u.id,'手术帽入库','2026-03-02 08:00:00'
FROM materials m, users u WHERE m.material_code='MAT011' AND u.username='keeper1';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',300,'BATCH2025M07A',u.id,'导尿管补充','2026-03-02 09:30:00'
FROM materials m, users u WHERE m.material_code='MAT007' AND u.username='keeper2';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',500,'BATCH2025M11A',d.id,u.id,'手术室领手术帽','2026-03-02 11:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT011' AND u.username='keeper1' AND d.dept_code='SHOUSHU';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',30,'BATCH2025M07A',d.id,u.id,'ICU导尿管','2026-03-02 14:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT007' AND u.username='keeper2' AND d.dept_code='ICU';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',100,'BATCH2025M12A',d.id,u.id,'骨科领用纱布','2026-03-02 15:30:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT012' AND u.username='keeper1' AND d.dept_code='GUTKE';

-- 2026-03-03 (周二)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',400,'BATCH2026M01A',u.id,'输液器月度入库','2026-03-03 08:30:00'
FROM materials m, users u WHERE m.material_code='MAT004' AND u.username='keeper1';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',200,'BATCH2026M02A',u.id,'医用纱布入库','2026-03-03 10:00:00'
FROM materials m, users u WHERE m.material_code='MAT003' AND u.username='keeper2';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',150,'BATCH2026M01A',d.id,u.id,'心内科领输液器','2026-03-03 13:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT004' AND u.username='keeper1' AND d.dept_code='XINNEIKE';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',100,'BATCH2026M02A',d.id,u.id,'急诊领用纱布','2026-03-03 14:30:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT003' AND u.username='keeper2' AND d.dept_code='JIZHEN';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',200,'BATCH2025M09A',d.id,u.id,'儿科领用棉球','2026-03-03 16:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT009' AND u.username='keeper1' AND d.dept_code='ERKE';

-- 2026-03-04 (周三，今天)
INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',600,'BATCH2026M03A',u.id,'注射器月度补货','2026-03-04 08:00:00'
FROM materials m, users u WHERE m.material_code='MAT001' AND u.username='keeper1';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, operator_id, remark, create_time)
SELECT m.id,'INBOUND',300,'BATCH2026M04A',u.id,'医用手套入库','2026-03-04 09:00:00'
FROM materials m, users u WHERE m.material_code='MAT002' AND u.username='keeper2';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',80,'BATCH2026M03A',d.id,u.id,'妇产科领用','2026-03-04 10:00:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT001' AND u.username='keeper1' AND d.dept_code='FUCHANK';

INSERT INTO inventory_transactions (material_id, transaction_type, quantity, batch_number, dept_id, operator_id, remark, create_time)
SELECT m.id,'OUTBOUND',100,'BATCH2026M04A',d.id,u.id,'放射科领用手套','2026-03-04 11:30:00'
FROM materials m, users u, departments d WHERE m.material_code='MAT002' AND u.username='keeper2' AND d.dept_code='FANGSHE';
