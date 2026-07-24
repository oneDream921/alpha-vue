package io.github.onedream921.alphavue.framework.security;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resolves roles and menu permissions for Sa-Token from the relational schema. */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final JdbcTemplate jdbcTemplate;

    public StpInterfaceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT m.permission
                        FROM sys_menu m
                        JOIN sys_role_menu rm ON rm.menu_id = m.id
                        JOIN sys_user_role ur ON ur.role_id = rm.role_id
                        WHERE ur.user_id = ?
                          AND m.deleted = 0
                          AND m.status = 1
                          AND m.permission IS NOT NULL
                        """, String.class, loginId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return jdbcTemplate.queryForList("""
                        SELECT r.code
                        FROM sys_role r
                        JOIN sys_user_role ur ON ur.role_id = r.id
                        WHERE ur.user_id = ? AND r.deleted = 0 AND r.status = 1
                        """, String.class, loginId);
    }
}
