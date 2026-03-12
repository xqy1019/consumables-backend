-- =====================================================
-- V8: 补充注册证/经营许可证数据 + 召回示例数据
-- =====================================================

-- =====================================================
-- 1. 更新耗材：注册证信息 + 生产厂家 + 是否高值
-- =====================================================
-- 注射类
UPDATE materials SET
  registration_no = 'K20230001',
  registration_expiry = '2028-06-30',
  manufacturer = '江苏康进医疗器械有限公司',
  is_high_value = false
WHERE material_code = 'MAT001';

UPDATE materials SET
  registration_no = 'K20220015',
  registration_expiry = '2027-09-30',
  manufacturer = '上海医安医疗器械有限公司',
  is_high_value = false
WHERE material_code = 'MAT013';  -- 静脉留置针

-- 防护类
UPDATE materials SET
  registration_no = 'K20220089',
  registration_expiry = '2027-03-31',
  manufacturer = '广东洁特生物过滤股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT002';

UPDATE materials SET
  registration_no = 'K20240122',
  registration_expiry = '2029-08-31',
  manufacturer = '振德医疗用品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT006';  -- 医用外科口罩

UPDATE materials SET
  registration_no = 'K20210056',
  registration_expiry = '2026-05-14',   -- 即将到期（<60天，约70天后到期 → 已<60天）
  manufacturer = '稳健医疗用品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT010';  -- 外科手套

UPDATE materials SET
  registration_no = 'K20230099',
  registration_expiry = '2028-12-31',
  manufacturer = '振德医疗用品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT011';  -- 手术帽

UPDATE materials SET
  registration_no = 'K20190033',
  registration_expiry = '2025-12-31',   -- 已过期
  manufacturer = '奥美医疗用品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT020';  -- 医用弹力袜

-- 敷料类
UPDATE materials SET
  registration_no = 'K20220044',
  registration_expiry = '2027-06-30',
  manufacturer = '稳健医疗用品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT003';

UPDATE materials SET
  registration_no = 'K20230067',
  registration_expiry = '2028-09-30',
  manufacturer = '振德医疗用品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT012';  -- 止血纱布

-- 输液类
UPDATE materials SET
  registration_no = 'K20220078',
  registration_expiry = '2027-11-30',
  manufacturer = '山东威高集团医用高分子制品股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT004';

-- 检测/监测类
UPDATE materials SET
  registration_no = 'K20180021',
  registration_expiry = '2026-04-20',   -- 即将到期（<60天）
  manufacturer = '深圳迈瑞生物医疗电子股份有限公司',
  is_high_value = true
WHERE material_code = 'MAT005';  -- 血压计袖带

UPDATE materials SET
  registration_no = 'K20200088',
  registration_expiry = '2027-05-31',
  manufacturer = '深圳迈瑞生物医疗电子股份有限公司',
  is_high_value = true
WHERE material_code = 'MAT008';  -- 心电监护导联线

UPDATE materials SET
  registration_no = 'K20230154',
  registration_expiry = '2028-10-31',
  manufacturer = '深圳迈瑞生物医疗电子股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT024';  -- 心电图电极片

-- 导管类
UPDATE materials SET
  registration_no = 'K20220031',
  registration_expiry = '2027-08-31',
  manufacturer = '广东百合医疗科技股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT007';  -- 导尿管

UPDATE materials SET
  registration_no = 'K20210079',
  registration_expiry = '2026-12-31',
  manufacturer = '广东百合医疗科技股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT016';  -- 引流袋

-- 消毒类
UPDATE materials SET
  registration_no = 'K20230201',
  registration_expiry = '2028-03-31',
  manufacturer = '江苏利康医用耗材有限公司',
  is_high_value = false
WHERE material_code = 'MAT009';  -- 医用棉球

UPDATE materials SET
  registration_no = 'K20220166',
  registration_expiry = '2027-07-31',
  manufacturer = '天津博朗医疗科技有限公司',
  is_high_value = false
WHERE material_code = 'MAT015';  -- 酒精棉片

-- 骨科类
UPDATE materials SET
  registration_no = 'K20230045',
  registration_expiry = '2028-05-31',
  manufacturer = '北京安贞骨科器械有限公司',
  is_high_value = false
WHERE material_code = 'MAT017';  -- 骨科固定绷带

-- 急救类
UPDATE materials SET
  registration_no = 'K20200112',
  registration_expiry = '2026-03-10',   -- 极近到期（<30天）
  manufacturer = '飞利浦（中国）投资有限公司',
  is_high_value = true
WHERE material_code = 'MAT018';  -- 除颤仪电极片

-- 外科类
UPDATE materials SET
  registration_no = 'K20230088',
  registration_expiry = '2028-07-31',
  manufacturer = '苏州英途康医疗器械有限公司',
  is_high_value = false
WHERE material_code = 'MAT019';  -- 负压引流球

-- 呼吸类
UPDATE materials SET
  registration_no = 'K20220097',
  registration_expiry = '2027-04-30',
  manufacturer = '鱼跃医疗设备股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT021';  -- 雾化吸入面罩

-- 检验类
UPDATE materials SET
  registration_no = 'K20230199',
  registration_expiry = '2028-11-30',
  manufacturer = '安图生物工程（郑州）股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT014';  -- 真空采血管

UPDATE materials SET
  registration_no = 'K20220141',
  registration_expiry = '2027-10-31',
  manufacturer = '三诺生物传感股份有限公司',
  is_high_value = false
WHERE material_code = 'MAT022';  -- 血糖检测试纸

-- 产科类
UPDATE materials SET
  registration_no = 'K20230077',
  registration_expiry = '2028-06-30',
  manufacturer = '珠海科域生物工程有限公司',
  is_high_value = false
WHERE material_code = 'MAT023';  -- 胎心监护耦合剂

-- 内镜类
UPDATE materials SET
  registration_no = 'K20210033',
  registration_expiry = '2026-04-30',   -- 即将到期（<60天）
  manufacturer = '南京微创医学科技股份有限公司',
  is_high_value = true
WHERE material_code = 'MAT025';  -- 内镜活检钳

-- =====================================================
-- 2. 更新供应商：经营许可证信息
-- =====================================================
UPDATE suppliers SET
  license_no = '(京)食药监械经营许可证第20180088号',
  license_expiry = '2028-07-15'
WHERE supplier_code = 'SUP001';

UPDATE suppliers SET
  license_no = '(沪)食药监械经营许可证第20190044号',
  license_expiry = '2026-04-10'           -- 即将到期（<60天）
WHERE supplier_code = 'SUP002';

UPDATE suppliers SET
  license_no = '(苏)食药监械经营许可证第20200136号',
  license_expiry = '2028-12-31'
WHERE supplier_code = 'SUP003';

UPDATE suppliers SET
  license_no = '(粤)食药监械经营许可证第20210077号',
  license_expiry = '2026-03-20'           -- 已过期
WHERE supplier_code = 'SUP004';

UPDATE suppliers SET
  license_no = '(京)食药监械经营许可证第20220055号',
  license_expiry = '2027-08-20'
WHERE supplier_code = 'SUP005';

-- =====================================================
-- 3. 插入召回通知示例数据
-- =====================================================
INSERT INTO recall_notices (recall_no, title, recall_reason, recall_level, source, issued_date, status, remark, created_by, create_time)
SELECT
  'RC202602180001',
  '除颤仪电极片批次召回通知',
  '生产商飞利浦公司发现批次BATCH2025M18A的除颤仪电极片导电膏可能存在固化不均匀问题，导致粘附力不足，影响除颤效果，存在患者安全隐患。',
  'I',
  'SUPPLIER',
  '2026-02-18',
  'ACTIVE',
  '已通知各科室暂停使用，等待处置',
  u.id,
  '2026-02-18 10:00:00'
FROM users u WHERE u.username = 'admin';

INSERT INTO recall_notice_batches (recall_id, material_id, batch_number, quantity_affected, remark)
SELECT rn.id, m.id, 'BATCH2025M18A', 30, '当前库存全部批次受影响'
FROM recall_notices rn, materials m
WHERE rn.recall_no = 'RC202602180001' AND m.material_code = 'MAT018';

INSERT INTO recall_disposals (recall_id, material_id, inventory_id, batch_number, quantity, disposal_type, disposal_date, remark, operator_id)
SELECT
  rn.id, m.id, inv.id, 'BATCH2025M18A', 10, 'QUARANTINE',
  '2026-02-19 14:00:00',
  '隔离10件待生产商检测确认',
  u.id
FROM recall_notices rn, materials m, inventory inv, users u
WHERE rn.recall_no = 'RC202602180001'
  AND m.material_code = 'MAT018'
  AND inv.material_id = m.id
  AND inv.batch_number = 'BATCH2025M18A'
  AND u.username = 'keeper1';

-- 召回2: 已关闭的历史召回（医用外科口罩）
INSERT INTO recall_notices (recall_no, title, recall_reason, recall_level, source, issued_date, status, remark, created_by, create_time)
SELECT
  'RC202601080001',
  '医用外科口罩（批次BATCH2024M06B）召回',
  '国家药监局抽检发现该批次口罩过滤效率不达标（实测值81%，低于≥95%标准），已对同批产品全部召回。',
  'II',
  'REGULATOR',
  '2026-01-08',
  'CLOSED',
  '已完成退货处置，召回关闭',
  u.id,
  '2026-01-08 09:00:00'
FROM users u WHERE u.username = 'admin';

INSERT INTO recall_notice_batches (recall_id, material_id, batch_number, quantity_affected, remark)
SELECT rn.id, m.id, 'BATCH2024M06B', 200, '该批次过滤效率不达标'
FROM recall_notices rn, materials m
WHERE rn.recall_no = 'RC202601080001' AND m.material_code = 'MAT006';

INSERT INTO recall_disposals (recall_id, material_id, inventory_id, batch_number, quantity, disposal_type, disposal_date, remark, operator_id)
SELECT
  rn.id, m.id, inv.id, 'BATCH2024M06B', 200, 'RETURN',
  '2026-01-12 10:00:00',
  '全部退货给供应商，已取得退货凭证',
  u.id
FROM recall_notices rn, materials m, inventory inv, users u
WHERE rn.recall_no = 'RC202601080001'
  AND m.material_code = 'MAT006'
  AND inv.material_id = m.id
  AND inv.batch_number = 'BATCH2024M06B'
  AND u.username = 'keeper1';

-- 召回3: 进行中的活跃召回（静脉留置针）
INSERT INTO recall_notices (recall_no, title, recall_reason, recall_level, source, issued_date, status, remark, created_by, create_time)
SELECT
  'RC202603010001',
  '静脉留置针批次质量问题召回',
  '使用科室反馈BATCH2025M13A批次留置针穿刺针尖存在毛刺，影响穿刺顺畅性，可能造成血管损伤。',
  'II',
  'INTERNAL',
  '2026-03-01',
  'ACTIVE',
  '已暂停发放，待处置',
  u.id,
  '2026-03-01 08:00:00'
FROM users u WHERE u.username = 'admin';

INSERT INTO recall_notice_batches (recall_id, material_id, batch_number, quantity_affected, remark)
SELECT rn.id, m.id, 'BATCH2025M13A', 500, '库存全部批次暂停使用'
FROM recall_notices rn, materials m
WHERE rn.recall_no = 'RC202603010001' AND m.material_code = 'MAT013';
