-- V17: 小耗材精细化管理演示数据

-- 科室定数配置（常用小耗材，覆盖4个典型科室）

-- ICU重症监护（dept_id=10）：用量最大，管控最严
INSERT INTO dept_par_levels (dept_id, material_id, par_quantity, min_quantity, monthly_limit) VALUES
(10, 1,  200, 50,  600),  -- 一次性注射器 5ml
(10, 2,  150, 30,  400),  -- 医用手套 M号
(10, 3,  300, 80,  800),  -- 医用纱布
(10, 4,  100, 20,  300),  -- 输液器
(10, 6,  200, 50,  500),  -- 医用外科口罩
(10, 7,   20,  5,   60);  -- 一次性导尿管

-- 内科（dept_id=2）
INSERT INTO dept_par_levels (dept_id, material_id, par_quantity, min_quantity, monthly_limit) VALUES
(2, 1,  100, 30,  300),   -- 一次性注射器
(2, 2,   80, 20,  200),   -- 医用手套
(2, 3,  150, 40,  400),   -- 医用纱布
(2, 4,   60, 15,  180),   -- 输液器
(2, 6,  100, 30,  300);   -- 医用外科口罩

-- 外科（dept_id=3）
INSERT INTO dept_par_levels (dept_id, material_id, par_quantity, min_quantity, monthly_limit) VALUES
(3, 1,  120, 30,  350),   -- 一次性注射器
(3, 2,  100, 25,  250),   -- 医用手套
(3, 3,  250, 60,  600),   -- 医用纱布（外科用量大）
(3, 10,  50, 10,  120),   -- 外科无菌手套
(3, 11, 200, 50,  500),   -- 一次性手术帽
(3, 6,  150, 40,  400);   -- 医用外科口罩

-- 急诊科（dept_id=4）
INSERT INTO dept_par_levels (dept_id, material_id, par_quantity, min_quantity, monthly_limit) VALUES
(4, 1,  150, 50,  450),   -- 一次性注射器（急诊用量波动大，限额放宽）
(4, 2,  120, 30,  350),   -- 医用手套
(4, 3,  200, 60,  600),   -- 医用纱布
(4, 4,   80, 20,  250),   -- 输液器
(4, 6,  200, 60,  600),   -- 医用外科口罩
(4, 7,   15,  5,   50);   -- 一次性导尿管

-- 诊疗消耗包明细（为初始化的6个模板配置标准耗材）
-- 模板ID: 1=静脉注射, 2=肌肉注射, 3=换药(小), 4=换药(大), 5=留置导尿, 6=静脉输液

-- 消耗包只包含"操作级"耗材（每次操作必然消耗的）
-- 口罩、手术帽等"班次级"耗材通过定数管理按班次领用，不计入操作消耗包

-- 静脉注射（template_id=1）：仅注射器（手套是否每次换由本院感控规范决定，不预设）
INSERT INTO procedure_template_items (template_id, material_id, quantity, note) VALUES
(1, 1,  1, '5ml注射器');

-- 肌肉注射（template_id=2）：注射器+纱布（手套同上，由本院规范决定）
INSERT INTO procedure_template_items (template_id, material_id, quantity, note) VALUES
(2, 1,  1, '5ml注射器'),
(2, 3,  2, '消毒纱布');

-- 换药（小）（template_id=3）：手套+纱布
INSERT INTO procedure_template_items (template_id, material_id, quantity, note) VALUES
(3, 2,  1, '操作手套'),
(3, 3,  4, '清洁纱布');

-- 换药（大）（template_id=4）：手套×2+纱布×8
INSERT INTO procedure_template_items (template_id, material_id, quantity, note) VALUES
(4, 2,  2, '内外层手套各1双'),
(4, 3,  8, '清洁纱布');

-- 留置导尿（template_id=5）：导尿管+手套×2+纱布
INSERT INTO procedure_template_items (template_id, material_id, quantity, note) VALUES
(5, 7,  1, '导尿管16Fr'),
(5, 2,  2, '无菌手套2双'),
(5, 3,  6, '无菌纱布');

-- 静脉输液（template_id=6）：输液器+留置针+手套
INSERT INTO procedure_template_items (template_id, material_id, quantity, note) VALUES
(6, 4,  1, '输液器'),
(6, 13, 1, '留置针'),
(6, 2,  1, '操作手套');
