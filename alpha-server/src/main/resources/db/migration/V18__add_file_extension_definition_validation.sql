UPDATE sys_config_definition
SET string_max_length = 219,
    string_pattern = '[a-z0-9]{1,10}(,[a-z0-9]{1,10}){0,19}'
WHERE config_key = 'file.upload.allowed-extensions' AND deleted = 0;
