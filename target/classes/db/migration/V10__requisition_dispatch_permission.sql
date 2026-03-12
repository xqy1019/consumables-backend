-- 新增「派发申领」操作权限
INSERT INTO permissions (permission_code, permission_name, type, description, sort_order)
VALUES ('requisition:dispatch', '派发申领', 'action', '发放申领单耗材给申领科室', 26);

-- 将 menu:requisition 授权给库管员（使其能访问申领管理页面执行派发操作）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'WAREHOUSE_KEEPER' AND p.permission_code = 'menu:requisition';

-- 将 requisition:dispatch 授权给库管员和管理员
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code IN ('WAREHOUSE_KEEPER', 'ADMIN') AND p.permission_code = 'requisition:dispatch';
