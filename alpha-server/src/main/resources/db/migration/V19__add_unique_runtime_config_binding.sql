CREATE UNIQUE INDEX uk_sys_config_definition_runtime_binding
    ON sys_config_definition (runtime_binding, deleted);
