INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 9, 3, 'Create user', 'BUTTON', 'system:user:create', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:user:create');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 10, 3, 'Update user', 'BUTTON', 'system:user:update', 0, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:user:update');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 11, 3, 'Delete user', 'BUTTON', 'system:user:delete', 0, 3 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:user:delete');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 12, 4, 'Create role', 'BUTTON', 'system:role:create', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:role:create');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 13, 4, 'Update role', 'BUTTON', 'system:role:update', 0, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:role:update');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 14, 4, 'Delete role', 'BUTTON', 'system:role:delete', 0, 3 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:role:delete');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 15, 4, 'Assign permissions', 'BUTTON', 'system:role:assign', 0, 4 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:role:assign');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 16, 5, 'Create menu', 'BUTTON', 'system:menu:create', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:menu:create');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 17, 5, 'Update menu', 'BUTTON', 'system:menu:update', 0, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:menu:update');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 18, 5, 'Delete menu', 'BUTTON', 'system:menu:delete', 0, 3 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:menu:delete');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 19, 6, 'Create department', 'BUTTON', 'system:dept:create', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dept:create');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 20, 6, 'Update department', 'BUTTON', 'system:dept:update', 0, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dept:update');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 21, 6, 'Delete department', 'BUTTON', 'system:dept:delete', 0, 3 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dept:delete');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 22, 7, 'Upload file', 'BUTTON', 'file:upload', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'file:upload');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 23, 7, 'Delete file', 'BUTTON', 'file:delete', 0, 2 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'file:delete');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 24, 8, 'Read login logs', 'BUTTON', 'log:login:list', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'log:login:list');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON rm.role_id = 1 AND rm.menu_id = m.id
WHERE rm.menu_id IS NULL;
