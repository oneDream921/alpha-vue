INSERT INTO sys_menu (parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 0, 'SQL 日志', 'MENU', '/monitor/sql', 'monitor/sql', 'monitor:sql:list', 'FileSearchOutlined', 1, 6
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:sql:list');

INSERT INTO sys_menu (parent_id, title, menu_type, permission, visible, sort_order)
SELECT parent.id, '清空 SQL 日志', 'BUTTON', 'monitor:sql:clear', 0, 1
FROM sys_menu parent
WHERE parent.permission = 'monitor:sql:list'
  AND parent.deleted = 0
  AND parent.status = 1
  AND parent.menu_type = 'MENU'
  AND parent.id = (SELECT MIN(candidate.id) FROM sys_menu candidate WHERE candidate.permission = 'monitor:sql:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:sql:clear');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission IN ('monitor:sql:list', 'monitor:sql:clear')
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
