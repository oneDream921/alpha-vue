INSERT INTO sys_menu (parent_id, title, menu_type, permission, visible, sort_order)
SELECT parent.id, '控制 SQL 日志采集', 'BUTTON', 'monitor:sql:control', 0, 2
FROM sys_menu parent
WHERE parent.permission = 'monitor:sql:list'
  AND parent.deleted = 0
  AND parent.status = 1
  AND parent.menu_type = 'MENU'
  AND parent.id = (SELECT MIN(candidate.id) FROM sys_menu candidate WHERE candidate.permission = 'monitor:sql:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:sql:control');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission = 'monitor:sql:control'
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
