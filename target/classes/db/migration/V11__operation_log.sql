-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    dept_name VARCHAR(100),
    module VARCHAR(50),
    action VARCHAR(100),
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    request_params TEXT,
    response_code INTEGER,
    ip_addr VARCHAR(50),
    status INTEGER DEFAULT 1,
    error_msg TEXT,
    duration_ms BIGINT,
    operate_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_op_log_time ON operation_log(operate_time);
CREATE INDEX IF NOT EXISTS idx_op_log_user ON operation_log(username);
CREATE INDEX IF NOT EXISTS idx_op_log_module ON operation_log(module);

-- 操作日志菜单权限
INSERT INTO permissions (permission_code, permission_name, type, description, sort_order)
VALUES ('menu:system:log', '操作日志', 'menu', '操作日志查看', 14)
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'menu:system:log'
ON CONFLICT DO NOTHING;
