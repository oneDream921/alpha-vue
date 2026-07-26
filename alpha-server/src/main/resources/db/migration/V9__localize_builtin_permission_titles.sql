UPDATE sys_menu
SET title = CASE permission
    WHEN 'system:user:create' THEN '新增用户'
    WHEN 'system:user:update' THEN '修改用户'
    WHEN 'system:user:delete' THEN '删除用户'
    WHEN 'system:role:create' THEN '新增角色'
    WHEN 'system:role:update' THEN '修改角色'
    WHEN 'system:role:delete' THEN '删除角色'
    WHEN 'system:role:assign' THEN '分配角色权限'
    WHEN 'system:menu:create' THEN '新增菜单'
    WHEN 'system:menu:update' THEN '修改菜单'
    WHEN 'system:menu:delete' THEN '删除菜单'
    WHEN 'system:dept:create' THEN '新增部门'
    WHEN 'system:dept:update' THEN '修改部门'
    WHEN 'system:dept:delete' THEN '删除部门'
    WHEN 'file:upload' THEN '上传文件'
    WHEN 'file:delete' THEN '删除文件'
    WHEN 'log:login:list' THEN '查看登录日志'
    WHEN 'log:operation:handle' THEN '处理操作日志'
    WHEN 'system:user:reset-password' THEN '重置用户密码'
    ELSE title
END
WHERE permission IN (
    'system:user:create',
    'system:user:update',
    'system:user:delete',
    'system:role:create',
    'system:role:update',
    'system:role:delete',
    'system:role:assign',
    'system:menu:create',
    'system:menu:update',
    'system:menu:delete',
    'system:dept:create',
    'system:dept:update',
    'system:dept:delete',
    'file:upload',
    'file:delete',
    'log:login:list',
    'log:operation:handle',
    'system:user:reset-password'
);
