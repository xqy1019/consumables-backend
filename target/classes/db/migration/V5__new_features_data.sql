-- =====================================================
-- V5: 新功能模块初始化数据
-- =====================================================

-- =====================================================
-- 1. 字典数据
-- =====================================================
INSERT INTO sys_dict (dict_name, dict_code, dict_type, remark) VALUES
('耗材分类', 'material_category', 'normal', '医疗耗材分类字典'),
('库存操作类型', 'inventory_type', 'normal', '库存操作类型'),
('报损原因', 'damage_reason', 'normal', '耗材报损原因'),
('采购状态', 'purchase_status', 'normal', '采购业务状态'),
('手术类型', 'surgery_type', 'normal', '手术分类'),
('性别', 'gender', 'normal', '患者性别')
ON CONFLICT DO NOTHING;

-- 耗材分类字典项
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order)
SELECT d.id, v.label, v.value, v.sort_order FROM sys_dict d,
(VALUES
  ('注射类','injection',1),('防护类','protection',2),('敷料类','dressing',3),
  ('输液类','infusion',4),('检测类','detection',5),('导管类','catheter',6),
  ('消毒类','disinfection',7),('监测类','monitoring',8),('骨科类','orthopedics',9),
  ('急救类','emergency',10),('外科类','surgery',11),('呼吸类','respiratory',12),
  ('检验类','laboratory',13),('产科类','obstetrics',14),('内镜类','endoscopy',15),('其他','other',16)
) AS v(label, value, sort_order)
WHERE d.dict_code = 'material_category' ON CONFLICT DO NOTHING;

-- 库存操作类型
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order)
SELECT d.id, v.label, v.value, v.sort_order FROM sys_dict d,
(VALUES ('入库','INBOUND',1),('出库','OUTBOUND',2),('盘点','STOCKTAKING',3),('移库','TRANSFER',4),('报损','DAMAGE',5),('借用','BORROWING',6))
AS v(label, value, sort_order)
WHERE d.dict_code = 'inventory_type' ON CONFLICT DO NOTHING;

-- 报损原因
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order)
SELECT d.id, v.label, v.value, v.sort_order FROM sys_dict d,
(VALUES ('过期损坏','EXPIRED',1),('物理损坏','PHYSICAL',2),('质量问题','QUALITY',3),('存储不当','STORAGE',4),('其他原因','OTHER',5))
AS v(label, value, sort_order)
WHERE d.dict_code = 'damage_reason' ON CONFLICT DO NOTHING;

-- 手术类型
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order)
SELECT d.id, v.label, v.value, v.sort_order FROM sys_dict d,
(VALUES ('骨科手术','ORTHOPEDIC',1),('普外科手术','GENERAL',2),('心胸外科','CARDIOTHORACIC',3),('神经外科','NEUROSURGERY',4),('妇产科手术','OBSTETRIC',5),('其他','OTHER',6))
AS v(label, value, sort_order)
WHERE d.dict_code = 'surgery_type' ON CONFLICT DO NOTHING;

-- 性别
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order)
SELECT d.id, v.label, v.value, v.sort_order FROM sys_dict d,
(VALUES ('男','M',1),('女','F',2))
AS v(label, value, sort_order)
WHERE d.dict_code = 'gender' ON CONFLICT DO NOTHING;

-- =====================================================
-- 2. 高值耗材 UDI 数据（内镜活检钳、除颤仪电极片、心电监护导联线）
-- =====================================================
INSERT INTO material_udi (material_id, inventory_id, udi_code, batch_number, serial_number, manufacture_date, expiry_date, supplier_id, status)
SELECT m.id, i.id, CONCAT('UDI-', m.material_code, '-', LPAD(gs::text, 4, '0')),
       i.batch_number, CONCAT('SN', m.material_code, LPAD(gs::text, 6, '0')),
       i.manufacture_date, i.expiry_date, i.supplier_id, 'IN_STOCK'
FROM materials m
JOIN inventory i ON i.material_id = m.id
CROSS JOIN generate_series(1, CASE WHEN m.material_code = 'MAT025' THEN 4
                                    WHEN m.material_code = 'MAT018' THEN 10
                                    WHEN m.material_code = 'MAT008' THEN 5
                                    ELSE 0 END) AS gs
WHERE m.material_code IN ('MAT025', 'MAT018', 'MAT008')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 3. 手术记录（10条示例）
-- =====================================================
INSERT INTO surgery_record (surgery_no, patient_id, patient_name, patient_age, patient_gender, dept_id, surgery_date, surgery_type, doctor_name, status, remark, created_by)
SELECT v.surgery_no, v.patient_id, v.patient_name, v.patient_age, v.patient_gender, d.id, v.surgery_date::TIMESTAMP, v.surgery_type, v.doctor_name, 'COMPLETED', v.remark, u.id
FROM (VALUES
  ('SUR202601001','P20260101','张明华',45,'M',(SELECT id FROM departments WHERE dept_code='WAIKE'),'2026-01-10 08:00:00','GENERAL','李大夫','普外科手术，阑尾切除','WAIKE'),
  ('SUR202601002','P20260102','王秀芳',38,'F',(SELECT id FROM departments WHERE dept_code='GUTKE'),'2026-01-12 09:30:00','ORTHOPEDIC','赵主任','骨科手术，骨折内固定','GUTKE'),
  ('SUR202601003','P20260103','刘建国',62,'M',(SELECT id FROM departments WHERE dept_code='WAIKE'),'2026-01-15 10:00:00','GENERAL','张医生','普外科胆囊切除','WAIKE'),
  ('SUR202601004','P20260104','陈美丽',29,'F',(SELECT id FROM departments WHERE dept_code='FUCHANK'),'2026-01-18 08:30:00','OBSTETRIC','周主任','妇科腹腔镜手术','FUCHANK'),
  ('SUR202601005','P20260105','孙志远',55,'M',(SELECT id FROM departments WHERE dept_code='WAIKE'),'2026-01-22 09:00:00','GENERAL','李大夫','肠道手术','WAIKE'),
  ('SUR202602001','P20260201','吴晓燕',42,'F',(SELECT id FROM departments WHERE dept_code='GUTKE'),'2026-02-05 10:00:00','ORTHOPEDIC','赵主任','膝关节置换手术','GUTKE'),
  ('SUR202602002','P20260202','郑建军',67,'M',(SELECT id FROM departments WHERE dept_code='WAIKE'),'2026-02-08 08:30:00','CARDIOTHORACIC','王教授','心胸外科冠脉搭桥','WAIKE'),
  ('SUR202602003','P20260203','赵雅芳',35,'F',(SELECT id FROM departments WHERE dept_code='FUCHANK'),'2026-02-12 09:00:00','OBSTETRIC','周主任','剖宫产手术','FUCHANK'),
  ('SUR202603001','P20260301','周国强',50,'M',(SELECT id FROM departments WHERE dept_code='WAIKE'),'2026-03-01 10:30:00','GENERAL','张医生','胃部手术','WAIKE'),
  ('SUR202603002','P20260302','李雪梅',44,'F',(SELECT id FROM departments WHERE dept_code='GUTKE'),'2026-03-03 09:00:00','ORTHOPEDIC','赵主任','腰椎手术','GUTKE')
) AS v(surgery_no, patient_id, patient_name, patient_age, patient_gender, dept_id_val, surgery_date, surgery_type, doctor_name, remark, dept_code)
JOIN departments d ON d.dept_code = v.dept_code
JOIN users u ON u.username = 'keeper1'
ON CONFLICT DO NOTHING;

-- 手术耗材绑定（绑定高值UDI）
INSERT INTO surgery_material_binding (surgery_id, udi_id, material_id, quantity, use_date)
SELECT sr.id, mu.id, mu.material_id, 1, sr.surgery_date
FROM surgery_record sr, material_udi mu
WHERE sr.surgery_no = 'SUR202601001' AND mu.udi_code LIKE 'UDI-MAT025%'
LIMIT 2;

INSERT INTO surgery_material_binding (surgery_id, udi_id, material_id, quantity, use_date)
SELECT sr.id, mu.id, mu.material_id, 1, sr.surgery_date
FROM surgery_record sr, material_udi mu
WHERE sr.surgery_no = 'SUR202601002' AND mu.udi_code LIKE 'UDI-MAT018%'
LIMIT 1;

-- 普通耗材绑定（不带UDI）
INSERT INTO surgery_material_binding (surgery_id, material_id, quantity, use_date)
SELECT sr.id, m.id, 5, sr.surgery_date
FROM surgery_record sr, materials m
WHERE sr.surgery_no = 'SUR202601001' AND m.material_code = 'MAT010';

INSERT INTO surgery_material_binding (surgery_id, material_id, quantity, use_date)
SELECT sr.id, m.id, 3, sr.surgery_date
FROM surgery_record sr, materials m
WHERE sr.surgery_no = 'SUR202601001' AND m.material_code = 'MAT012';

INSERT INTO surgery_material_binding (surgery_id, material_id, quantity, use_date)
SELECT sr.id, m.id, 10, sr.surgery_date
FROM surgery_record sr, materials m
WHERE sr.surgery_no = 'SUR202601002' AND m.material_code = 'MAT017';

INSERT INTO surgery_material_binding (surgery_id, material_id, quantity, use_date)
SELECT sr.id, m.id, 2, sr.surgery_date
FROM surgery_record sr, materials m
WHERE sr.surgery_no = 'SUR202602001' AND m.material_code = 'MAT017';

INSERT INTO surgery_material_binding (surgery_id, material_id, quantity, use_date)
SELECT sr.id, m.id, 1, sr.surgery_date
FROM surgery_record sr, materials m
WHERE sr.surgery_no = 'SUR202602002' AND m.material_code = 'MAT019';

-- =====================================================
-- 4. 盘点记录
-- =====================================================
INSERT INTO inventory_stocktaking (stocktaking_no, stocktaking_date, location, status, remark, created_by)
SELECT v.stocktaking_no, v.stocktaking_date::TIMESTAMP, v.location, v.status, v.remark, u.id
FROM (VALUES
  ('STCK202601001','2026-01-31 09:00:00','A区','COMPLETED','1月末全区盘点'),
  ('STCK202602001','2026-02-28 10:00:00','B区','COMPLETED','2月末B区盘点'),
  ('STCK202603001','2026-03-04 08:30:00','全库','IN_PROGRESS','3月季度盘点进行中')
) AS v(stocktaking_no, stocktaking_date, location, status, remark)
JOIN users u ON u.username = 'keeper1'
ON CONFLICT DO NOTHING;

-- 盘点明细
INSERT INTO inventory_stocktaking_item (stocktaking_id, material_id, inventory_id, batch_number, system_quantity, actual_quantity, difference)
SELECT st.id, i.material_id, i.id, i.batch_number, i.quantity, i.quantity, 0
FROM inventory_stocktaking st, inventory i
WHERE st.stocktaking_no = 'STCK202601001' AND i.status = 1
LIMIT 10;

INSERT INTO inventory_stocktaking_item (stocktaking_id, material_id, inventory_id, batch_number, system_quantity, actual_quantity, difference)
SELECT st.id, i.material_id, i.id, i.batch_number, i.quantity,
       CASE WHEN random() < 0.2 THEN i.quantity - FLOOR(random()*5 + 1)::INT ELSE i.quantity END,
       0
FROM inventory_stocktaking st, inventory i
WHERE st.stocktaking_no = 'STCK202602001' AND i.status = 1
LIMIT 8;

UPDATE inventory_stocktaking_item
SET difference = actual_quantity - system_quantity
WHERE difference = 0 AND actual_quantity IS NOT NULL;

-- =====================================================
-- 5. 移库记录
-- =====================================================
INSERT INTO inventory_transfer (transfer_no, material_id, inventory_id, quantity, from_location, to_location, transfer_date, status, remark, operator_id)
SELECT v.transfer_no, m.id, i.id, v.quantity, i.location, v.to_location, v.transfer_date::TIMESTAMP, 'COMPLETED', v.remark, u.id
FROM (VALUES
  ('TRF202601001','MAT006',100,'C区-01','2026-01-20 10:00:00','A区口罩移至C区备用'),
  ('TRF202602001','MAT009',200,'A区-04','2026-02-10 14:00:00','棉球移库至儿科备用区'),
  ('TRF202603001','MAT014',500,'A区-09','2026-03-02 09:00:00','采血管移库至检验科')
) AS v(transfer_no, mat_code, quantity, to_location, transfer_date, remark)
JOIN materials m ON m.material_code = v.mat_code
JOIN inventory i ON i.material_id = m.id AND i.status = 1
JOIN users u ON u.username = 'keeper1'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 6. 报损记录
-- =====================================================
INSERT INTO inventory_damage (damage_no, material_id, inventory_id, batch_number, quantity, damage_reason, damage_date, status, remark, operator_id)
SELECT v.damage_no, m.id, i.id, i.batch_number, v.quantity, v.reason, v.damage_date::TIMESTAMP, 'CONFIRMED', v.remark, u.id
FROM (VALUES
  ('DMG202601001','MAT006',50,'EXPIRED','2026-01-15 11:00:00','过期口罩销毁处理'),
  ('DMG202602001','MAT016',10,'PHYSICAL','2026-02-20 15:00:00','引流袋包装破损，无法使用'),
  ('DMG202603001','MAT008',2,'QUALITY','2026-03-01 09:30:00','导联线质量问题，厂家召回')
) AS v(damage_no, mat_code, quantity, reason, damage_date, remark)
JOIN materials m ON m.material_code = v.mat_code
JOIN inventory i ON i.material_id = m.id AND i.status = 1
JOIN users u ON u.username = 'keeper1'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 7. 借用记录
-- =====================================================
INSERT INTO inventory_borrowing (borrowing_no, material_id, inventory_id, batch_number, quantity, dept_id, borrower_name, borrowing_date, expected_return_date, actual_return_date, status, remark, operator_id)
SELECT v.borrowing_no, m.id, i.id, i.batch_number, v.quantity, d.id, v.borrower, v.borrowing_date::TIMESTAMP, v.expected_date::DATE,
       CASE WHEN v.status = 'RETURNED' THEN (v.expected_date::DATE - 2) ELSE NULL END,
       v.status, v.remark, u.id
FROM (VALUES
  ('BRW202601001','MAT018',2,'ICU','急诊科护士长陈丽','2026-01-25 09:00:00','2026-02-25','RETURNED','除颤仪电极片急用借用','JIZHEN'),
  ('BRW202602001','MAT008',3,'ICU','护士长王五','2026-02-15 14:00:00','2026-03-15','RETURNED','心电导联线借用','NEIKE'),
  ('BRW202603001','MAT025',2,'手术室','外科护士长周颖','2026-03-01 10:00:00','2026-03-31','BORROWED','活检钳借用待归还','WAIKE')
) AS v(borrowing_no, mat_code, quantity, from_dept, borrower, borrowing_date, expected_date, status, remark, dept_code)
JOIN materials m ON m.material_code = v.mat_code
JOIN inventory i ON i.material_id = m.id AND i.status = 1
JOIN departments d ON d.dept_code = v.dept_code
JOIN users u ON u.username = 'keeper1'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 8. AI 预测结果（基于历史流水数据生成）
-- =====================================================
INSERT INTO ai_prediction_result (material_id, prediction_month, predicted_quantity, actual_quantity, confidence, algorithm)
SELECT m.id, '2026-01',
       ROUND((RANDOM() * 200 + 100))::INT,
       ROUND((RANDOM() * 200 + 100))::INT,
       ROUND((75 + RANDOM() * 20)::NUMERIC, 2),
       'MA3'
FROM materials m WHERE m.status = 1
ON CONFLICT DO NOTHING;

INSERT INTO ai_prediction_result (material_id, prediction_month, predicted_quantity, actual_quantity, confidence, algorithm)
SELECT m.id, '2026-02',
       ROUND((RANDOM() * 200 + 100))::INT,
       ROUND((RANDOM() * 200 + 100))::INT,
       ROUND((78 + RANDOM() * 18)::NUMERIC, 2),
       'MA3'
FROM materials m WHERE m.status = 1
ON CONFLICT DO NOTHING;

INSERT INTO ai_prediction_result (material_id, prediction_month, predicted_quantity, confidence, algorithm)
SELECT m.id, '2026-03',
       ROUND((RANDOM() * 200 + 100))::INT,
       ROUND((80 + RANDOM() * 15)::NUMERIC, 2),
       'MA3'
FROM materials m WHERE m.status = 1
ON CONFLICT DO NOTHING;

INSERT INTO ai_prediction_result (material_id, prediction_month, predicted_quantity, confidence, algorithm)
SELECT m.id, '2026-04',
       ROUND((RANDOM() * 200 + 100))::INT,
       ROUND((77 + RANDOM() * 18)::NUMERIC, 2),
       'EWMA'
FROM materials m WHERE m.status = 1
ON CONFLICT DO NOTHING;

-- =====================================================
-- 9. 采购请购单（3条）
-- =====================================================
INSERT INTO purchase_requisition (req_no, dept_id, req_date, required_date, status, total_amount, remark, created_by)
SELECT v.req_no, d.id, v.req_date::TIMESTAMP, v.required_date::DATE, v.status, v.total_amount, v.remark, u.id
FROM (VALUES
  ('PUR202601001','WAREHOUSE','2026-01-20 09:00:00','2026-02-10','APPROVED',3500.00,'一月库存补充请购'),
  ('PUR202602001','WAREHOUSE','2026-02-05 10:00:00','2026-02-28','APPROVED',8200.00,'高值耗材补充采购'),
  ('PUR202603001','WAREHOUSE','2026-03-01 09:00:00','2026-03-20','PENDING',5600.00,'3月常规耗材采购')
) AS v(req_no, dept_code, req_date, required_date, status, total_amount, remark)
JOIN departments d ON d.dept_code = v.dept_code
JOIN users u ON u.username = 'purchaser1'
ON CONFLICT DO NOTHING;

-- 请购单明细
INSERT INTO purchase_requisition_item (req_id, material_id, quantity, estimated_price, subtotal)
SELECT r.id, m.id, 1000, m.standard_price, 1000 * m.standard_price
FROM purchase_requisition r, materials m
WHERE r.req_no = 'PUR202601001' AND m.material_code = 'MAT001';

INSERT INTO purchase_requisition_item (req_id, material_id, quantity, estimated_price, subtotal)
SELECT r.id, m.id, 500, m.standard_price, 500 * m.standard_price
FROM purchase_requisition r, materials m
WHERE r.req_no = 'PUR202601001' AND m.material_code = 'MAT004';

INSERT INTO purchase_requisition_item (req_id, material_id, quantity, estimated_price, subtotal)
SELECT r.id, m.id, 30, m.standard_price, 30 * m.standard_price
FROM purchase_requisition r, materials m
WHERE r.req_no = 'PUR202602001' AND m.material_code = 'MAT018';

INSERT INTO purchase_requisition_item (req_id, material_id, quantity, estimated_price, subtotal)
SELECT r.id, m.id, 20, m.standard_price, 20 * m.standard_price
FROM purchase_requisition r, materials m
WHERE r.req_no = 'PUR202602001' AND m.material_code = 'MAT025';

INSERT INTO purchase_requisition_item (req_id, material_id, quantity, estimated_price, subtotal)
SELECT r.id, m.id, 2000, m.standard_price, 2000 * m.standard_price
FROM purchase_requisition r, materials m
WHERE r.req_no = 'PUR202603001' AND m.material_code = 'MAT002';

INSERT INTO purchase_requisition_item (req_id, material_id, quantity, estimated_price, subtotal)
SELECT r.id, m.id, 200, m.standard_price, 200 * m.standard_price
FROM purchase_requisition r, materials m
WHERE r.req_no = 'PUR202603001' AND m.material_code = 'MAT007';

-- 更新请购单 approved_by
UPDATE purchase_requisition
SET approved_by = (SELECT id FROM users WHERE username = 'admin'),
    approved_time = req_date + INTERVAL '1 day',
    approval_remark = '审核通过，同意采购'
WHERE status = 'APPROVED';

-- =====================================================
-- 10. 询价单
-- =====================================================
INSERT INTO purchase_inquiry (inquiry_no, req_id, supplier_id, inquiry_date, valid_date, status, total_amount, remark, created_by)
SELECT v.inquiry_no, r.id, s.id, v.inquiry_date::TIMESTAMP, v.valid_date::DATE, v.status, v.total_amount, v.remark, u.id
FROM (VALUES
  ('INQ202601001','PUR202601001','SUP001','2026-01-22 10:00:00','2026-02-22','CONFIRMED',2800.00,'供应商A报价'),
  ('INQ202601002','PUR202601001','SUP002','2026-01-22 10:30:00','2026-02-22','CONFIRMED',3100.00,'供应商B报价'),
  ('INQ202602001','PUR202602001','SUP004','2026-02-07 09:00:00','2026-03-07','CONFIRMED',8000.00,'高值耗材询价'),
  ('INQ202603001','PUR202603001','SUP001','2026-03-03 10:00:00','2026-04-03','SENT',5400.00,'3月询价单')
) AS v(inquiry_no, req_no, sup_code, inquiry_date, valid_date, status, total_amount, remark)
JOIN purchase_requisition r ON r.req_no = v.req_no
JOIN suppliers s ON s.supplier_code = v.sup_code
JOIN users u ON u.username = 'purchaser1'
ON CONFLICT DO NOTHING;

-- 询价单明细
INSERT INTO purchase_inquiry_item (inquiry_id, material_id, quantity, quoted_price, subtotal)
SELECT i.id, m.id, 1000, 0.48, 480.00
FROM purchase_inquiry i, materials m
WHERE i.inquiry_no = 'INQ202601001' AND m.material_code = 'MAT001';

INSERT INTO purchase_inquiry_item (inquiry_id, material_id, quantity, quoted_price, subtotal)
SELECT i.id, m.id, 500, 2.40, 1200.00
FROM purchase_inquiry i, materials m
WHERE i.inquiry_no = 'INQ202601001' AND m.material_code = 'MAT004';

INSERT INTO purchase_inquiry_item (inquiry_id, material_id, quantity, quoted_price, subtotal)
SELECT i.id, m.id, 30, 82.00, 2460.00
FROM purchase_inquiry i, materials m
WHERE i.inquiry_no = 'INQ202602001' AND m.material_code = 'MAT018';

INSERT INTO purchase_inquiry_item (inquiry_id, material_id, quantity, quoted_price, subtotal)
SELECT i.id, m.id, 20, 115.00, 2300.00
FROM purchase_inquiry i, materials m
WHERE i.inquiry_no = 'INQ202602001' AND m.material_code = 'MAT025';

-- =====================================================
-- 11. 采购合同
-- =====================================================
INSERT INTO purchase_contract (contract_no, inquiry_id, supplier_id, contract_date, delivery_date, total_amount, status, remark, created_by)
SELECT v.contract_no, i.id, s.id, v.contract_date::DATE, v.delivery_date::DATE, v.total_amount, v.status, v.remark, u.id
FROM (VALUES
  ('CON202601001','INQ202601001','SUP001','2026-01-25','2026-02-10',2800.00,'EXECUTED','1月采购合同，已执行'),
  ('CON202602001','INQ202602001','SUP004','2026-02-10','2026-02-28',8000.00,'EXECUTED','高值耗材采购合同'),
  ('CON202603001','INQ202603001','SUP001','2026-03-04','2026-03-20',5400.00,'ACTIVE','3月采购合同，进行中')
) AS v(contract_no, inquiry_no, sup_code, contract_date, delivery_date, total_amount, status, remark)
JOIN purchase_inquiry i ON i.inquiry_no = v.inquiry_no
JOIN suppliers s ON s.supplier_code = v.sup_code
JOIN users u ON u.username = 'purchaser1'
ON CONFLICT DO NOTHING;

-- 合同明细
INSERT INTO purchase_contract_item (contract_id, material_id, quantity, unit_price, total_price, delivered_quantity)
SELECT c.id, m.id, 1000, 0.48, 480.00, 1000
FROM purchase_contract c, materials m
WHERE c.contract_no = 'CON202601001' AND m.material_code = 'MAT001';

INSERT INTO purchase_contract_item (contract_id, material_id, quantity, unit_price, total_price, delivered_quantity)
SELECT c.id, m.id, 500, 2.40, 1200.00, 500
FROM purchase_contract c, materials m
WHERE c.contract_no = 'CON202601001' AND m.material_code = 'MAT004';

INSERT INTO purchase_contract_item (contract_id, material_id, quantity, unit_price, total_price, delivered_quantity)
SELECT c.id, m.id, 30, 82.00, 2460.00, 30
FROM purchase_contract c, materials m
WHERE c.contract_no = 'CON202602001' AND m.material_code = 'MAT018';

INSERT INTO purchase_contract_item (contract_id, material_id, quantity, unit_price, total_price, delivered_quantity)
SELECT c.id, m.id, 20, 115.00, 2300.00, 20
FROM purchase_contract c, materials m
WHERE c.contract_no = 'CON202602001' AND m.material_code = 'MAT025';

INSERT INTO purchase_contract_item (contract_id, material_id, quantity, unit_price, total_price, delivered_quantity)
SELECT c.id, m.id, 2000, 1.15, 2300.00, 0
FROM purchase_contract c, materials m
WHERE c.contract_no = 'CON202603001' AND m.material_code = 'MAT002';
