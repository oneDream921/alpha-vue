CREATE TABLE sys_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dict_type_code_deleted UNIQUE (type_code, deleted)
);

CREATE TABLE sys_dict_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_id BIGINT NOT NULL,
    label VARCHAR(64) NOT NULL,
    dict_value VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    is_default TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dict_item_type_value_deleted UNIQUE (type_id, dict_value, deleted)
);

CREATE INDEX idx_sys_dict_type_deleted_name ON sys_dict_type (deleted, type_name, id);
CREATE INDEX idx_sys_dict_item_type_deleted_sort ON sys_dict_item (type_id, deleted, sort_order, id);

INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 31, 2, '数据字典', 'MENU', 'dicts', 'system/dicts', 'system:dict:list', 'BookOutlined', 1, 6
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:list');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 32, 31, '新增数据字典', 'BUTTON', 'system:dict:create', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:create');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 33, 31, '修改数据字典', 'BUTTON', 'system:dict:update', 0, 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:update');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 34, 31, '删除数据字典', 'BUTTON', 'system:dict:delete', 0, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission IN (
    'system:dict:list', 'system:dict:create', 'system:dict:update', 'system:dict:delete'
)
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
