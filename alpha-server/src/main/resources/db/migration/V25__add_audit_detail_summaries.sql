ALTER TABLE sys_oper_log ADD COLUMN request_summary TEXT NULL;
ALTER TABLE sys_oper_log ADD COLUMN response_summary VARCHAR(2000) NULL;

INSERT INTO sys_menu (parent_id, title, menu_type, permission, visible, sort_order)
SELECT parent.id, 'View operation log detail', 'BUTTON', 'log:operation:detail', 0, 3
FROM sys_menu parent
WHERE parent.permission = 'log:operation:list'
  AND parent.deleted = 0
  AND parent.status = 1
  AND parent.menu_type = 'MENU'
  AND parent.id = (SELECT MIN(candidate.id) FROM sys_menu candidate WHERE candidate.permission = 'log:operation:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'log:operation:detail');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON rm.role_id = 1 AND rm.menu_id = m.id
WHERE m.permission = 'log:operation:detail' AND rm.menu_id IS NULL;
