INSERT INTO sys_config_definition (config_key, config_name, config_group, value_type, default_value,
                                   enum_values, `sensitive`, `dynamic`, status)
SELECT 'cache.display.captcha', '验证码缓存展示级别', 'cache', 'ENUM', 'HIDDEN',
       'HIDDEN,MASKED,PLAIN', 0, 0, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM sys_config_definition WHERE config_key = 'cache.display.captcha' AND deleted = 0);

INSERT INTO sys_config_definition (config_key, config_name, config_group, value_type, default_value,
                                   enum_values, `sensitive`, `dynamic`, status)
SELECT 'cache.display.login-failure', '登录失败窗口展示级别', 'cache', 'ENUM', 'HIDDEN',
       'HIDDEN,MASKED,PLAIN', 0, 0, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM sys_config_definition WHERE config_key = 'cache.display.login-failure' AND deleted = 0);

INSERT INTO sys_config_definition (config_key, config_name, config_group, value_type, default_value,
                                   enum_values, `sensitive`, `dynamic`, status)
SELECT 'cache.display.session', 'Sa-Token 会话展示级别', 'cache', 'ENUM', 'HIDDEN',
       'HIDDEN,MASKED,PLAIN', 0, 0, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM sys_config_definition WHERE config_key = 'cache.display.session' AND deleted = 0);
