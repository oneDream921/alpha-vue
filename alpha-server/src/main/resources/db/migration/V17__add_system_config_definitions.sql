CREATE TABLE sys_config_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(128) NOT NULL,
    config_name VARCHAR(64) NOT NULL,
    config_group VARCHAR(64) NOT NULL,
    value_type VARCHAR(16) NOT NULL,
    default_value TEXT NOT NULL,
    integer_min INT NULL,
    integer_max INT NULL,
    string_max_length INT NULL,
    string_pattern VARCHAR(500) NULL,
    enum_values VARCHAR(2000) NULL,
    `sensitive` TINYINT NOT NULL DEFAULT 0,
    `dynamic` TINYINT NOT NULL DEFAULT 0,
    runtime_binding VARCHAR(64) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_config_definition_key_deleted UNIQUE (config_key, deleted)
);

INSERT INTO sys_config_definition (config_key, config_name, config_group, value_type, default_value,
                                   integer_min, integer_max, `sensitive`, `dynamic`, runtime_binding, status)
VALUES
    ('file.upload.max-size-mb', '单文件上传大小（MB）', 'file', 'INTEGER', '10', 1, 100, 0, 1, 'FILE_UPLOAD_MAX_SIZE', 'PUBLISHED'),
    ('file.upload.allowed-extensions', '允许上传的扩展名', 'file', 'STRING', 'jpg,jpeg,png,pdf,doc,docx,xls,xlsx', NULL, NULL, 0, 1, 'FILE_UPLOAD_ALLOWED_EXTENSIONS', 'PUBLISHED'),
    ('file.private-access-ttl-minutes', '私有文件访问期限（分钟）', 'file', 'INTEGER', '15', 1, 1440, 0, 1, 'FILE_PRIVATE_ACCESS_TTL', 'PUBLISHED');

INSERT INTO sys_menu (parent_id, title, menu_type, permission, visible, sort_order)
SELECT 27, '定义参数配置', 'BUTTON', 'system:config:define', 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:config:define');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission = 'system:config:define'
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
