ALTER TABLE sys_config ADD COLUMN config_name VARCHAR(64) NULL;
ALTER TABLE sys_config ADD COLUMN config_group VARCHAR(64) NOT NULL DEFAULT 'general';
ALTER TABLE sys_config ADD COLUMN data_type VARCHAR(16) NOT NULL DEFAULT 'STRING';
ALTER TABLE sys_config ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1;

UPDATE sys_config
SET config_name = config_key
WHERE config_name IS NULL;

ALTER TABLE sys_config
    MODIFY COLUMN config_name VARCHAR(64) NOT NULL;
