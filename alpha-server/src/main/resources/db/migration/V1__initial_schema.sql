CREATE TABLE migration_probe (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_dept_parent_id ON sys_dept (parent_id);
CREATE INDEX idx_sys_dept_deleted ON sys_dept (deleted);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar VARCHAR(255),
    email VARCHAR(128),
    phone VARCHAR(32),
    dept_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    must_change_password TINYINT NOT NULL DEFAULT 1,
    last_login_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE INDEX idx_sys_user_dept_id ON sys_user (dept_id);
CREATE INDEX idx_sys_user_deleted ON sys_user (deleted);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE INDEX idx_sys_role_deleted ON sys_role (deleted);

CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(64) NOT NULL,
    menu_type VARCHAR(16) NOT NULL,
    path VARCHAR(128),
    component VARCHAR(255),
    permission VARCHAR(128),
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_menu_parent_id ON sys_menu (parent_id);
CREATE INDEX idx_sys_menu_permission ON sys_menu (permission);
CREATE INDEX idx_sys_menu_deleted ON sys_menu (deleted);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id)
);

CREATE INDEX idx_sys_role_menu_menu_id ON sys_role_menu (menu_id);

CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(500),
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

CREATE INDEX idx_sys_config_deleted ON sys_config (deleted);

CREATE TABLE sys_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    storage_provider VARCHAR(32) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    size_bytes BIGINT NOT NULL,
    public_url VARCHAR(1024),
    uploader_id BIGINT,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_file_object_key UNIQUE (object_key)
);

CREATE INDEX idx_sys_file_uploader_id ON sys_file (uploader_id);
CREATE INDEX idx_sys_file_deleted ON sys_file (deleted);

CREATE TABLE sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64),
    user_id BIGINT,
    login_type VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(1000),
    message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_login_log_user_id_created_at ON sys_login_log (user_id, created_at);
CREATE INDEX idx_sys_login_log_created_at ON sys_login_log (created_at);

CREATE TABLE sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(64),
    module VARCHAR(64) NOT NULL,
    operation VARCHAR(128) NOT NULL,
    method VARCHAR(16),
    request_uri VARCHAR(255),
    request_params TEXT,
    response_code INT,
    status TINYINT NOT NULL,
    ip_address VARCHAR(64),
    duration_ms BIGINT,
    trace_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_oper_log_user_id_created_at ON sys_oper_log (user_id, created_at);
CREATE INDEX idx_sys_oper_log_trace_id ON sys_oper_log (trace_id);
CREATE INDEX idx_sys_oper_log_created_at ON sys_oper_log (created_at);

INSERT INTO sys_user (id, username, password, nickname, must_change_password)
VALUES (1, 'admin', '$2a$10$v6eFc6AgyU7o6oIjdA/V1eJctWdbQX9ydbfXfQd0JMht/trbUgurO', 'Administrator', 1);

INSERT INTO sys_role (id, name, code, sort_order)
VALUES (1, 'Super Administrator', 'SUPER_ADMIN', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, permission, icon, sort_order) VALUES
    (1, 0, 'Dashboard', 'MENU', '/home', 'home/index', NULL, 'DashboardOutlined', 1),
    (2, 0, 'System', 'MENU', '/system', 'Layout', NULL, 'SettingOutlined', 2),
    (3, 2, 'Users', 'MENU', 'users', 'system/users', 'system:user:list', 'UserOutlined', 1),
    (4, 2, 'Roles', 'MENU', 'roles', 'system/roles', 'system:role:list', 'SafetyOutlined', 2),
    (5, 2, 'Menus', 'MENU', 'menus', 'system/menus', 'system:menu:list', 'MenuOutlined', 3),
    (6, 2, 'Departments', 'MENU', 'depts', 'system/depts', 'system:dept:list', 'ApartmentOutlined', 4),
    (7, 0, 'Files', 'MENU', '/files', 'files/index', 'file:list', 'FolderOpenOutlined', 3),
    (8, 0, 'Logs', 'MENU', '/logs', 'system/logs', 'log:operation:list', 'FileTextOutlined', 4);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4),
    (1, 5), (1, 6), (1, 7), (1, 8);
