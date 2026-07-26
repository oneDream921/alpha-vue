UPDATE sys_menu
SET title = CASE title
    WHEN 'Dashboard' THEN '工作台'
    WHEN 'System' THEN '系统管理'
    WHEN 'Users' THEN '用户管理'
    WHEN 'Roles' THEN '角色管理'
    WHEN 'Menus' THEN '菜单管理'
    WHEN 'Departments' THEN '部门管理'
    WHEN 'Files' THEN '文件管理'
    WHEN 'Logs' THEN '审计日志'
    ELSE title
END
WHERE id BETWEEN 1 AND 8
  AND title IN ('Dashboard', 'System', 'Users', 'Roles', 'Menus', 'Departments', 'Files', 'Logs');
