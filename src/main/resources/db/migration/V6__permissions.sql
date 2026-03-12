-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'menu',
    description VARCHAR(200),
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 初始化权限数据（菜单权限）
INSERT INTO permissions (permission_code, permission_name, type, description, sort_order) VALUES
('menu:dashboard',      '工作台',     'menu', '工作台首页',       1),
('menu:material',       '耗材目录',   'menu', '耗材目录查看',     2),
('menu:dict',           '字典管理',   'menu', '数据字典管理',     3),
('menu:supplier',       '供应商管理', 'menu', '供应商管理',       4),
('menu:department',     '科室管理',   'menu', '科室管理',         5),
('menu:inventory',      '库存管理',   'menu', '库存管理查看',     6),
('menu:requisition',    '申领管理',   'menu', '申领单管理',       7),
('menu:tracing',        '高值追溯',   'menu', '高值耗材追溯',     8),
('menu:ai',             'AI智能',     'menu', 'AI需求预测与预警', 9),
('menu:purchase',       '采购管理',   'menu', '采购管理',         10),
('menu:report',         '统计报表',   'menu', '统计报表查看',     11),
('menu:system:user',    '用户管理',   'menu', '用户管理',         12),
('menu:system:role',    '角色管理',   'menu', '角色权限管理',     13);

-- 操作权限
INSERT INTO permissions (permission_code, permission_name, type, description, sort_order) VALUES
('material:edit',       '耗材编辑',   'action', '新增/编辑/删除耗材',   20),
('inventory:edit',      '库存操作',   'action', '出入库/移库/报损等',   21),
('requisition:approve', '审批申领',   'action', '审批申领单',           22),
('purchase:edit',       '采购编辑',   'action', '创建/编辑采购单',      23),
('user:edit',           '用户编辑',   'action', '新增/编辑/删除用户',   24),
('role:edit',           '角色编辑',   'action', '新增/编辑角色权限',    25);

-- ADMIN角色：拥有全部权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.role_code = 'ADMIN';

-- 科室主任：工作台、耗材(只看)、库存(只看)、申领管理+审批、报表
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'DEPT_DIRECTOR'
  AND p.permission_code IN (
    'menu:dashboard', 'menu:material', 'menu:inventory',
    'menu:requisition', 'requisition:approve', 'menu:report'
  );

-- 护士长：工作台、耗材(只看)、库存管理+操作、申领管理、高值追溯
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'HEAD_NURSE'
  AND p.permission_code IN (
    'menu:dashboard', 'menu:material', 'menu:inventory', 'inventory:edit',
    'menu:requisition', 'menu:tracing'
  );

-- 库管员：工作台、耗材(只看)、库存管理+操作、高值追溯
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'WAREHOUSE_KEEPER'
  AND p.permission_code IN (
    'menu:dashboard', 'menu:material', 'menu:inventory', 'inventory:edit', 'menu:tracing'
  );

-- 采购员：工作台、耗材(只看)、库存(只看)、采购管理+编辑
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'PURCHASER'
  AND p.permission_code IN (
    'menu:dashboard', 'menu:material', 'menu:inventory',
    'menu:purchase', 'purchase:edit'
  );

-- 财务人员：工作台、耗材(只看)、库存(只看)、报表
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'FINANCE'
  AND p.permission_code IN (
    'menu:dashboard', 'menu:material', 'menu:inventory', 'menu:report'
  );
