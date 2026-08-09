CREATE TABLE sys_oauth_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(32) NOT NULL,
    subject VARCHAR(191) NOT NULL,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(128) NULL,
    avatar_url VARCHAR(1024) NULL,
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_oauth_account_subject_deleted UNIQUE (provider, subject, deleted)
);

CREATE INDEX idx_sys_oauth_account_user_id ON sys_oauth_account (user_id);
