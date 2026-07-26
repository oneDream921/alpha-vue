INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 35, 0, 'Redis 管理', 'MENU', '/monitor/redis', 'monitor/redis', 'monitor:redis:list', 'DatabaseOutlined', 1, 5
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:redis:list');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 36, 35, '删除 Redis 键', 'BUTTON', 'monitor:redis:delete', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:redis:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission IN ('monitor:redis:list', 'monitor:redis:delete')
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
