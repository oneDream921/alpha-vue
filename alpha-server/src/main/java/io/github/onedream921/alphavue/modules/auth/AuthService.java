package io.github.onedream921.alphavue.modules.auth;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.auth.dto.LoginRequest;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.github.onedream921.alphavue.modules.log.AuditLogService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Password login and profile queries. Passwords never leave this service. */
@Service
public class AuthService {

    private static final long SESSION_TIMEOUT_SECONDS = 8 * 60 * 60;

    private final JdbcTemplate jdbcTemplate;
    private final LoginFailureStore loginFailureStore;
    private final AuditLogService auditLogService;

    public AuthService(JdbcTemplate jdbcTemplate, LoginFailureStore loginFailureStore, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.loginFailureStore = loginFailureStore;
        this.auditLogService = auditLogService;
    }

    public LoginResponse login(LoginRequest request, String ipAddress) {
        if (!loginFailureStore.reserveAttempt(request.username(), ipAddress)) {
            auditLogService.recordLogin(request.username(), null, false, ipAddress);
            throw new BusinessException(429, PublicErrorMessage.LOGIN_TEMPORARILY_LOCKED);
        }

        UserAccount account = findAccount(request.username());
        if (account == null || !BCrypt.checkpw(request.password(), account.passwordHash())) {
            auditLogService.recordLogin(request.username(), null, false, ipAddress);
            throw new BusinessException(401, PublicErrorMessage.INVALID_CREDENTIALS);
        }

        loginFailureStore.clear(request.username(), ipAddress);
        StpUtil.login(account.id());
        auditLogService.recordLogin(account.username(), account.id(), true, ipAddress);
        return new LoginResponse(StpUtil.getTokenValue(), "Bearer", SESSION_TIMEOUT_SECONDS);
    }

    public Profile profile() {
        long userId = currentUserId();
        return jdbcTemplate.query("""
                        SELECT id, username, nickname, avatar, email, phone, dept_id, must_change_password
                        FROM sys_user
                        WHERE id = ? AND deleted = 0 AND status = 1
                        """, resultSet -> resultSet.next()
                ? new Profile(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("nickname"),
                        resultSet.getString("avatar"),
                        resultSet.getString("email"),
                        resultSet.getString("phone"),
                        resultSet.getObject("dept_id", Long.class),
                        resultSet.getBoolean("must_change_password"))
                : null, userId);
    }

    public List<Map<String, Object>> routes() {
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT m.id, m.parent_id, m.title, m.menu_type, m.path, m.component,
                               m.permission, m.icon, m.sort_order
                        FROM sys_menu m
                        JOIN sys_role_menu rm ON rm.menu_id = m.id
                        JOIN sys_role r ON r.id = rm.role_id
                        JOIN sys_user_role ur ON ur.role_id = rm.role_id
                        JOIN sys_user u ON u.id = ur.user_id
                        WHERE ur.user_id = ?
                          AND u.deleted = 0
                          AND u.status = 1
                          AND r.deleted = 0
                          AND r.status = 1
                          AND m.deleted = 0
                          AND m.status = 1
                          AND m.visible = 1
                        ORDER BY m.sort_order, m.id
                        """, currentUserId());
    }

    private UserAccount findAccount(String username) {
        List<UserAccount> accounts = jdbcTemplate.query("""
                        SELECT id, username, password
                        FROM sys_user
                        WHERE username = ? AND deleted = 0 AND status = 1
                        """, (resultSet, rowNum) -> new UserAccount(
                resultSet.getLong("id"), resultSet.getString("username"), resultSet.getString("password")), username);
        return accounts.isEmpty() ? null : accounts.getFirst();
    }

    private long currentUserId() {
        return Long.parseLong(StpUtil.getLoginIdAsString());
    }

    public interface LoginFailureStore {
        /** Atomically reserves one of the five permitted attempts in the fifteen-minute window. */
        boolean reserveAttempt(String username, String ipAddress);

        void clear(String username, String ipAddress);
    }

    private record UserAccount(long id, String username, String passwordHash) {
    }

    public record Profile(long id, String username, String nickname, String avatar, String email, String phone,
                          Long deptId, boolean mustChangePassword) {
    }
}
