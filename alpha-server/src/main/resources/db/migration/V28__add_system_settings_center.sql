CREATE TABLE sys_system_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_group VARCHAR(32) NOT NULL,
    values_json TEXT NOT NULL,
    secrets_ciphertext TEXT NULL,
    key_version INT NOT NULL DEFAULT 1,
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_system_setting_group_deleted UNIQUE (setting_group, deleted)
);

INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 50, 2, '系统配置', 'MENU', 'settings', 'system/settings', 'system:setting:list', 'SettingOutlined', 1, 5
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:setting:list');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 51, 50, '修改系统配置', 'BUTTON', 'system:setting:update', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:setting:update');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.permission IN ('system:setting:list', 'system:setting:update')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
