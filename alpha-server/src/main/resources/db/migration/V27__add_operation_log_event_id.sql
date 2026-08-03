ALTER TABLE sys_oper_log ADD COLUMN event_id VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_sys_oper_log_event_id ON sys_oper_log (event_id);
