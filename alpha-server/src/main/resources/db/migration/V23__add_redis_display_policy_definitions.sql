INSERT INTO sys_config_definition (config_key, config_name, config_group, value_type, default_value,
                                   enum_values, `sensitive`, `dynamic`, status)
SELECT 'cache.display.dictionary', '数据字典缓存展示级别', 'cache', 'ENUM', 'MASKED',
       'HIDDEN,MASKED,PLAIN', 0, 0, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM sys_config_definition WHERE config_key = 'cache.display.dictionary' AND deleted = 0);

INSERT INTO sys_config_definition (config_key, config_name, config_group, value_type, default_value,
                                   enum_values, `sensitive`, `dynamic`, status)
SELECT 'cache.display.business', '普通业务缓存展示级别', 'cache', 'ENUM', 'MASKED',
       'HIDDEN,MASKED,PLAIN', 0, 0, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM sys_config_definition WHERE config_key = 'cache.display.business' AND deleted = 0);
