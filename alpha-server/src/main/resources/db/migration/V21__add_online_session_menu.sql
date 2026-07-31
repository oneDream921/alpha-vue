INSERT INTO sys_menu (parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 0, '在线用户', 'MENU', '/monitor/online-users', 'monitor/online-users', 'monitor:online:list', 'TeamOutlined', 1, 4
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:online:list');

INSERT INTO sys_menu (parent_id, title, menu_type, permission, visible, sort_order)
SELECT parent.id, '定向下线', 'BUTTON', 'monitor:online:kickout', 0, 1
FROM sys_menu parent
WHERE parent.permission = 'monitor:online:list'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'monitor:online:kickout');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission IN ('monitor:online:list', 'monitor:online:kickout')
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
