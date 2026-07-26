ALTER TABLE sys_oper_log ADD COLUMN handling_status TINYINT NOT NULL DEFAULT 0;
UPDATE sys_oper_log SET handling_status = 1 WHERE handled = 1;
CREATE INDEX idx_sys_oper_log_handling_status_created_at ON sys_oper_log (handling_status, created_at);
