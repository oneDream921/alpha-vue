ALTER TABLE sys_oper_log ADD COLUMN exception_stack TEXT NULL;
ALTER TABLE sys_oper_log ADD COLUMN handled TINYINT NOT NULL DEFAULT 0;
ALTER TABLE sys_oper_log ADD COLUMN handled_by BIGINT NULL;
ALTER TABLE sys_oper_log ADD COLUMN handled_at TIMESTAMP NULL;

CREATE INDEX idx_sys_oper_log_status_handled_created_at ON sys_oper_log (status, handled, created_at);
