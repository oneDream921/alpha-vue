package io.github.onedream921.alphavue.modules.log;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Persists metadata-only audit events off the request thread. */
@Service
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async
    public void recordLogin(String username, Long userId, boolean succeeded, String ipAddress) {
        jdbcTemplate.update("""
                        INSERT INTO sys_login_log (username, user_id, login_type, status, ip_address, message)
                        VALUES (?, ?, 'PASSWORD', ?, ?, ?)
                        """,
                username, userId, succeeded ? 1 : 0, ipAddress,
                succeeded ? "Login succeeded" : "Login rejected");
    }

    @Async
    public void recordOperation(Long userId, String username, String module, String operation,
            String method, String requestUri, int responseCode, boolean succeeded,
            String ipAddress, long durationMs, String traceId) {
        jdbcTemplate.update("""
                        INSERT INTO sys_oper_log
                        (user_id, username, module, operation, method, request_uri, request_params,
                         response_code, status, ip_address, duration_ms, trace_id)
                        VALUES (?, ?, ?, ?, ?, ?, '[redacted]', ?, ?, ?, ?, ?)
                        """,
                userId, username, module, operation, method, requestUri, responseCode,
                succeeded ? 1 : 0, ipAddress, durationMs, traceId);
    }
}
