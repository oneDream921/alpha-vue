ALTER TABLE sys_login_log ADD COLUMN client_id VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN device_id VARCHAR(128);
ALTER TABLE sys_login_log ADD COLUMN device_name VARCHAR(128);
ALTER TABLE sys_login_log ADD COLUMN browser VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN operating_system VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN trace_id VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN error_message VARCHAR(500);
ALTER TABLE sys_login_log ADD COLUMN location VARCHAR(128);

ALTER TABLE sys_oper_log ADD COLUMN client_id VARCHAR(64);
ALTER TABLE sys_oper_log ADD COLUMN device_id VARCHAR(128);
ALTER TABLE sys_oper_log ADD COLUMN device_name VARCHAR(128);
ALTER TABLE sys_oper_log ADD COLUMN browser VARCHAR(64);
ALTER TABLE sys_oper_log ADD COLUMN operating_system VARCHAR(64);
ALTER TABLE sys_oper_log ADD COLUMN error_code INT;
ALTER TABLE sys_oper_log ADD COLUMN location VARCHAR(128);

CREATE INDEX idx_sys_login_log_trace_id ON sys_login_log (trace_id);
CREATE INDEX idx_sys_oper_log_client_id_created_at ON sys_oper_log (client_id, created_at);
