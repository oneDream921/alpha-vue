CREATE TABLE sys_client (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_client_client_id_deleted UNIQUE (client_id, deleted)
);

CREATE INDEX idx_sys_client_status_deleted ON sys_client (status, deleted);

INSERT INTO sys_client (client_id, name) VALUES ('pc-admin', 'PC 管理端');
