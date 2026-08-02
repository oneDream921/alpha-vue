package io.github.onedream921.alphavue.modules.maintenance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "alpha.maintenance.logs.dry-run=false",
        "alpha.maintenance.logs.retention-days=1",
        "alpha.maintenance.logs.batch-size=10"
})
@ActiveProfiles("test")
class LogRetentionMaintenanceIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogRetentionMaintenanceService service;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM sys_login_log WHERE username LIKE 'maintenance-%'");
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE username LIKE 'maintenance-%'");
    }

    @Test
    void deletesOnlyExpiredLoginAndSafeOperationLogs() {
        LocalDateTime old = LocalDateTime.now().minusDays(2);
        LocalDateTime recent = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO sys_login_log (username, login_type, status, message, created_at)
                VALUES ('maintenance-old-login', 'PASSWORD', 1, 'old', ?)
                """, old);
        jdbcTemplate.update("""
                INSERT INTO sys_login_log (username, login_type, status, message, created_at)
                VALUES ('maintenance-recent-login', 'PASSWORD', 1, 'recent', ?)
                """, recent);
        insertOperation("maintenance-success-operation", 1, 0, old);
        insertOperation("maintenance-handled-failure", 0, 1, old);
        insertOperation("maintenance-open-failure", 0, 0, old);
        insertOperation("maintenance-recent-success", 1, 0, recent);

        MaintenanceTaskReport report = service.run();

        assertThat(report.affected()).isEqualTo(3);
        assertThat(count("sys_login_log", "maintenance-old-login")).isZero();
        assertThat(count("sys_login_log", "maintenance-recent-login")).isEqualTo(1);
        assertThat(count("sys_oper_log", "maintenance-success-operation")).isZero();
        assertThat(count("sys_oper_log", "maintenance-handled-failure")).isZero();
        assertThat(count("sys_oper_log", "maintenance-open-failure")).isEqualTo(1);
        assertThat(count("sys_oper_log", "maintenance-recent-success")).isEqualTo(1);
    }

    private void insertOperation(String username, int status, int handlingStatus, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO sys_oper_log (username, module, operation, business_type, status, handling_status,
                                         method, request_uri, created_at)
                VALUES (?, 'MaintenanceTest', 'Retention', 'OTHER', ?, ?, 'TEST', '/test', ?)
                """, username, status, handlingStatus, createdAt);
    }

    private int count(String table, String username) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE username = ?", Integer.class,
                username);
    }
}
