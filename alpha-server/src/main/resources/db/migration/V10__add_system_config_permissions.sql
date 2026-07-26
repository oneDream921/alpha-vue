INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 27, 2, '参数配置', 'MENU', 'configs', 'system/configs', 'system:config:list', 'SettingOutlined', 1, 5
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:config:list');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 28, 27, '新增参数配置', 'BUTTON', 'system:config:create', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:config:create');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 29, 27, '修改参数配置', 'BUTTON', 'system:config:update', 0, 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:config:update');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 30, 27, '删除参数配置', 'BUTTON', 'system:config:delete', 0, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:config:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission IN (
    'system:config:list', 'system:config:create', 'system:config:update', 'system:config:delete'
)
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
