INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 26, 3, 'Reset user password', 'BUTTON', 'system:user:reset-password', 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:user:reset-password');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON rm.role_id = 1 AND rm.menu_id = m.id
WHERE m.permission = 'system:user:reset-password' AND rm.menu_id IS NULL;
