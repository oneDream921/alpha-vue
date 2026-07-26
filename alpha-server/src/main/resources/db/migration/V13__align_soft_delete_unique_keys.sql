ALTER TABLE sys_user MODIFY deleted BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_role MODIFY deleted BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_config MODIFY deleted BIGINT NOT NULL DEFAULT 0;

ALTER TABLE sys_user DROP INDEX uk_sys_user_username;
ALTER TABLE sys_user ADD CONSTRAINT uk_sys_user_username_deleted UNIQUE (username, deleted);
ALTER TABLE sys_role DROP INDEX uk_sys_role_code;
ALTER TABLE sys_role ADD CONSTRAINT uk_sys_role_code_deleted UNIQUE (code, deleted);
ALTER TABLE sys_config DROP INDEX uk_sys_config_key;
ALTER TABLE sys_config ADD CONSTRAINT uk_sys_config_key_deleted UNIQUE (config_key, deleted);
