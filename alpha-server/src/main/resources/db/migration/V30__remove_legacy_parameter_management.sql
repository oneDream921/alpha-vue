DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT id FROM sys_menu WHERE permission IN (
    'system:config:list',
    'system:config:create',
    'system:config:update',
    'system:config:delete',
    'system:config:define'
    )
);

DELETE FROM sys_menu
WHERE permission IN (
    'system:config:list',
    'system:config:create',
    'system:config:update',
    'system:config:delete',
    'system:config:define'
);

DROP TABLE sys_config_definition;
DROP TABLE sys_config;
