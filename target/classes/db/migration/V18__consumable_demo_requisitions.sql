-- V18: 小耗材精细化管理 - 演示申领数据 + 操作记录
-- 补充4个月的DISPATCHED申领单，使异常看板和消耗统计有数据可视化

-- ===================== 2025年12月 =====================
-- ICU (dept_id=10)
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202512-001', 10, 'DISPATCHED', '2025-12-05 09:00:00', '2025-12-05 09:00:00', '2025-12-05 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,160),(2,115),(3,230),(4,75),(6,140),(7,14)) AS v(mid,qty);

-- 内科 (dept_id=2)
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202512-002', 2, 'DISPATCHED', '2025-12-06 09:00:00', '2025-12-06 09:00:00', '2025-12-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,80),(2,55),(3,115),(4,48),(6,75)) AS v(mid,qty);

-- 外科 (dept_id=3)
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202512-003', 3, 'DISPATCHED', '2025-12-06 09:00:00', '2025-12-06 09:00:00', '2025-12-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,95),(2,70),(3,185),(6,105)) AS v(mid,qty);

-- 急诊科 (dept_id=4)
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202512-004', 4, 'DISPATCHED', '2025-12-07 09:00:00', '2025-12-07 09:00:00', '2025-12-07 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,115),(2,90),(3,155),(4,60),(6,145),(7,9)) AS v(mid,qty);


-- ===================== 2026年1月 =====================
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202601-001', 10, 'DISPATCHED', '2026-01-06 09:00:00', '2026-01-06 09:00:00', '2026-01-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,175),(2,125),(3,250),(4,82),(6,155),(7,16)) AS v(mid,qty);

WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202601-002', 2, 'DISPATCHED', '2026-01-07 09:00:00', '2026-01-07 09:00:00', '2026-01-07 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,88),(2,62),(3,125),(4,52),(6,82)) AS v(mid,qty);

WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202601-003', 3, 'DISPATCHED', '2026-01-07 09:00:00', '2026-01-07 09:00:00', '2026-01-07 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,102),(2,76),(3,192),(6,112)) AS v(mid,qty);

WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202601-004', 4, 'DISPATCHED', '2026-01-08 09:00:00', '2026-01-08 09:00:00', '2026-01-08 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,122),(2,98),(3,162),(4,67),(6,152),(7,11)) AS v(mid,qty);


-- ===================== 2026年2月 =====================
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202602-001', 10, 'DISPATCHED', '2026-02-05 09:00:00', '2026-02-05 09:00:00', '2026-02-05 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,180),(2,120),(3,245),(4,83),(6,148),(7,15)) AS v(mid,qty);

WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202602-002', 2, 'DISPATCHED', '2026-02-06 09:00:00', '2026-02-06 09:00:00', '2026-02-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,82),(2,58),(3,118),(4,50),(6,78)) AS v(mid,qty);

WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202602-003', 3, 'DISPATCHED', '2026-02-06 09:00:00', '2026-02-06 09:00:00', '2026-02-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,98),(2,72),(3,188),(6,108)) AS v(mid,qty);

WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202602-004', 4, 'DISPATCHED', '2026-02-06 09:00:00', '2026-02-06 09:00:00', '2026-02-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,118),(2,92),(3,158),(4,63),(6,148),(7,10)) AS v(mid,qty);


-- ===================== 2026年3月（当月，含异常数据） =====================
-- ICU: 注射器+65%, 纱布+58%, 导尿管+67% → DANGER
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202603-001', 10, 'DISPATCHED', '2026-03-05 09:00:00', '2026-03-05 09:00:00', '2026-03-05 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,285),(2,130),(3,385),(4,88),(6,160),(7,25)) AS v(mid,qty);

-- 内科: 输液器+50% → DANGER
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202603-002', 2, 'DISPATCHED', '2026-03-06 09:00:00', '2026-03-06 09:00:00', '2026-03-06 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,90),(2,65),(3,125),(4,76),(6,85)) AS v(mid,qty);

-- 外科: 手套+40% → WARNING
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202603-003', 3, 'DISPATCHED', '2026-03-07 09:00:00', '2026-03-07 09:00:00', '2026-03-07 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,105),(2,105),(3,195),(6,115)) AS v(mid,qty);

-- 急诊科: 注射器+42%, 手套+37% → WARNING
WITH ins AS (
  INSERT INTO requisitions (requisition_no, dept_id, status, requisition_date, create_time, update_time, created_by)
  VALUES ('DEMO-202603-004', 4, 'DISPATCHED', '2026-03-08 09:00:00', '2026-03-08 09:00:00', '2026-03-08 10:00:00', 1)
  RETURNING id
)
INSERT INTO requisition_items (requisition_id, material_id, quantity, actual_quantity)
SELECT ins.id, v.mid, v.qty, v.qty FROM ins,
  (VALUES (1,170),(2,130),(3,170),(4,70),(6,155),(7,12)) AS v(mid,qty);


-- ===================== 诊疗操作记录 =====================
-- 模板ID: 1=静脉注射, 2=肌肉注射, 3=换药(小), 4=换药(大), 5=留置导尿, 6=静脉输液
-- performed_by=2 (nurse1)

-- ICU (dept_id=10) - 高频操作
INSERT INTO procedure_records (dept_id, template_id, performed_by, performed_at, quantity, patient_info, note) VALUES
(10, 1, 2, '2026-03-01 08:30:00', 5, '1床-张三', '晨间静脉注射'),
(10, 6, 2, '2026-03-01 09:00:00', 3, '2床-李四', '输液治疗'),
(10, 3, 2, '2026-03-02 10:30:00', 2, '3床-王五', '伤口换药'),
(10, 5, 2, '2026-03-03 14:00:00', 1, '5床-赵六', '导尿术'),
(10, 1, 2, '2026-03-04 08:00:00', 8, NULL, '批量静脉注射'),
(10, 4, 2, '2026-03-05 15:00:00', 1, '1床-张三', '大换药'),
(10, 6, 2, '2026-03-06 09:00:00', 4, NULL, '上午输液'),
(10, 1, 2, '2026-03-08 08:30:00', 6, NULL, '晨间注射'),
(10, 3, 2, '2026-03-09 11:00:00', 3, '7床-周八', NULL),
(10, 6, 2, '2026-03-10 09:00:00', 5, NULL, '输液治疗');

-- 内科 (dept_id=2)
INSERT INTO procedure_records (dept_id, template_id, performed_by, performed_at, quantity, patient_info, note) VALUES
(2, 1, 2, '2026-03-01 09:00:00', 4, '12床-孙九', NULL),
(2, 6, 2, '2026-03-02 08:30:00', 3, NULL, '上午输液'),
(2, 2, 2, '2026-03-03 14:30:00', 2, '15床-钱十', '肌注'),
(2, 1, 2, '2026-03-05 08:00:00', 6, NULL, '批量注射'),
(2, 6, 2, '2026-03-07 09:00:00', 4, NULL, NULL),
(2, 3, 2, '2026-03-09 10:00:00', 1, '12床-孙九', '换药');

-- 外科 (dept_id=3)
INSERT INTO procedure_records (dept_id, template_id, performed_by, performed_at, quantity, patient_info, note) VALUES
(3, 4, 2, '2026-03-01 10:00:00', 2, '22床-吴一', '术后大换药'),
(3, 3, 2, '2026-03-02 14:00:00', 3, NULL, '小换药'),
(3, 4, 2, '2026-03-04 10:00:00', 1, '25床-郑二', NULL),
(3, 1, 2, '2026-03-06 08:30:00', 3, NULL, '静脉注射'),
(3, 3, 2, '2026-03-08 15:00:00', 4, NULL, '批量换药'),
(3, 5, 2, '2026-03-10 14:00:00', 1, '22床-吴一', '导尿');

-- 急诊科 (dept_id=4)
INSERT INTO procedure_records (dept_id, template_id, performed_by, performed_at, quantity, patient_info, note) VALUES
(4, 1, 2, '2026-03-01 22:00:00', 3, '急01-陈三', '急诊注射'),
(4, 6, 2, '2026-03-02 03:00:00', 2, '急02-黄四', '夜班输液'),
(4, 3, 2, '2026-03-03 16:00:00', 2, '急03-林五', '外伤换药'),
(4, 1, 2, '2026-03-05 20:00:00', 5, NULL, '夜班急诊'),
(4, 4, 2, '2026-03-07 08:00:00', 1, '急05-何六', '大面积换药'),
(4, 2, 2, '2026-03-09 14:00:00', 3, NULL, '肌注'),
(4, 6, 2, '2026-03-11 09:00:00', 3, NULL, '输液');

-- 2月的一些历史记录（让趋势更完整）
INSERT INTO procedure_records (dept_id, template_id, performed_by, performed_at, quantity, patient_info, note) VALUES
(10, 1, 2, '2026-02-10 08:30:00', 5, NULL, NULL),
(10, 6, 2, '2026-02-12 09:00:00', 3, NULL, NULL),
(10, 3, 2, '2026-02-15 10:00:00', 2, NULL, NULL),
(2,  1, 2, '2026-02-11 09:00:00', 4, NULL, NULL),
(2,  6, 2, '2026-02-14 08:30:00', 2, NULL, NULL),
(3,  4, 2, '2026-02-08 10:00:00', 2, NULL, NULL),
(3,  3, 2, '2026-02-13 14:00:00', 3, NULL, NULL),
(4,  1, 2, '2026-02-06 22:00:00', 4, NULL, NULL),
(4,  6, 2, '2026-02-09 09:00:00', 2, NULL, NULL);
