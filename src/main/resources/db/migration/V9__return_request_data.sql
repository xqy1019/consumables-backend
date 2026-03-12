-- =====================================================
-- V9: 退料申请示例数据
-- =====================================================

-- 退料单1: 待审批 - 内科（注射器多领）
INSERT INTO return_requests (return_no, dept_id, status, remark, created_by, create_time, update_time)
SELECT 'RET202602200001', d.id, 'PENDING', '本月注射器多领，退还50支', u.id,
       '2026-02-20 10:00:00', '2026-02-20 10:00:00'
FROM departments d, users u WHERE d.dept_code='NEIKE' AND u.username='nurse1';

INSERT INTO return_request_items (return_id, material_id, batch_number, quantity, remark)
SELECT r.id, m.id, 'BATCH2026M03A', 50, '注射器多领退还'
FROM return_requests r, materials m WHERE r.return_no='RET202602200001' AND m.material_code='MAT001';

-- 退料单2: 已审批 - 外科（手套近效期）
INSERT INTO return_requests (return_no, dept_id, status, remark, created_by, approved_by, approved_time, create_time, update_time)
SELECT 'RET202602150001', d.id, 'APPROVED', '手套批次BATCH2025M10A接近效期，退还库房统一处理',
       u.id, a.id, '2026-02-16 09:00:00', '2026-02-15 14:00:00', '2026-02-16 09:00:00'
FROM departments d, users u, users a
WHERE d.dept_code='WAIKE' AND u.username='nurse2' AND a.username='keeper1';

INSERT INTO return_request_items (return_id, material_id, batch_number, quantity, remark)
SELECT r.id, m.id, 'BATCH2025M10A', 100, '近效期退还'
FROM return_requests r, materials m WHERE r.return_no='RET202602150001' AND m.material_code='MAT010';

-- 退料单3: 已完成 - 急诊（棉球库存过剩）
INSERT INTO return_requests (return_no, dept_id, status, remark, created_by, approved_by, approved_time, create_time, update_time)
SELECT 'RET202601280001', d.id, 'COMPLETED', '急诊科棉球库存过剩，退还200袋',
       u.id, a.id, '2026-01-29 08:30:00', '2026-01-28 15:00:00', '2026-01-30 11:00:00'
FROM departments d, users u, users a
WHERE d.dept_code='JIZHEN' AND u.username='nurse3' AND a.username='keeper2';

INSERT INTO return_request_items (return_id, material_id, batch_number, quantity, remark)
SELECT r.id, m.id, 'BATCH2025M09A', 200, '过剩退还'
FROM return_requests r, materials m WHERE r.return_no='RET202601280001' AND m.material_code='MAT009';

-- 退料单4: 已驳回 - 骨科（数量有误）
INSERT INTO return_requests (return_no, dept_id, status, remark, created_by, approved_by, approved_time, create_time, update_time)
SELECT 'RET202602080001', d.id, 'REJECTED', '申请退料数量有误，驳回重新申请 | 审批意见：退料数量超过领用数量，请核实后重新提交',
       u.id, a.id, '2026-02-09 10:00:00', '2026-02-08 11:00:00', '2026-02-09 10:00:00'
FROM departments d, users u, users a
WHERE d.dept_code='GUTKE' AND u.username='nurse2' AND a.username='keeper1';

INSERT INTO return_request_items (return_id, material_id, batch_number, quantity, remark)
SELECT r.id, m.id, 'BATCH2025M17A', 500, '申请数量有误'
FROM return_requests r, materials m WHERE r.return_no='RET202602080001' AND m.material_code='MAT017';

-- 退料单5: 待审批 - 手术室（雾化面罩即将过期）
INSERT INTO return_requests (return_no, dept_id, status, remark, created_by, create_time, update_time)
SELECT 'RET202603010001', d.id, 'PENDING', '雾化面罩BATCH2024M21A即将过期，申请退还库房处理', u.id,
       '2026-03-01 09:00:00', '2026-03-01 09:00:00'
FROM departments d, users u WHERE d.dept_code='SHOUSHU' AND u.username='nurse2';

INSERT INTO return_request_items (return_id, material_id, batch_number, quantity, remark)
SELECT r.id, m.id, 'BATCH2024M21A', 20, '即将过期退还'
FROM return_requests r, materials m WHERE r.return_no='RET202603010001' AND m.material_code='MAT021';

INSERT INTO return_request_items (return_id, material_id, batch_number, quantity, remark)
SELECT r.id, m.id, 'BATCH2024M06B', 30, '召回批次退还'
FROM return_requests r, materials m WHERE r.return_no='RET202603010001' AND m.material_code='MAT006';
