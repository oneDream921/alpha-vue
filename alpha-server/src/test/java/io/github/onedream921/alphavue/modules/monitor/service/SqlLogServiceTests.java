package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.framework.mybatis.SqlLogCapture;
import io.github.onedream921.alphavue.modules.monitor.config.SqlMonitorProperties;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 日志服务测试。
 */
class SqlLogServiceTests {

    @Test
    void keepsBoundedRecentSqlWithoutParameterValues() {
        SqlMonitorProperties properties = new SqlMonitorProperties();
        properties.setMaxEntries(2);
        SqlLogService service = new SqlLogService(properties);

        service.record(new SqlLogCapture("UserMapper.selectById", "SELECT", "sys_user",
                "SELECT * FROM sys_user WHERE username = ?", 12, 1, "trace-1"));
        service.record(new SqlLogCapture("RoleMapper.selectById", "SELECT", "sys_role",
                "SELECT * FROM sys_role WHERE code = ?", 8, 1, "trace-2"));
        service.record(new SqlLogCapture("DeptMapper.selectById", "SELECT", "sys_dept",
                "SELECT * FROM sys_dept WHERE id = ?", 6, 1, "trace-3"));

        var logs = service.recent(new SqlLogQuery(10, "SELECT", null, false));

        assertThat(logs).hasSize(2);
        assertThat(logs).extracting("tableName").containsExactly("sys_dept", "sys_role");
        assertThat(logs).extracting("sql").allMatch(sql -> sql.toString().contains("?"));
        assertThat(logs).extracting("sql").doesNotContain("admin", "SUPER_ADMIN");
    }

    @Test
    void filtersBySlowSqlAndKeyword() {
        SqlMonitorProperties properties = new SqlMonitorProperties();
        properties.setSlowThresholdMs(50);
        SqlLogService service = new SqlLogService(properties);

        service.record(new SqlLogCapture("UserMapper.selectPage", "SELECT", "sys_user",
                "SELECT * FROM sys_user", 120, 10, "trace-user"));
        service.record(new SqlLogCapture("RoleMapper.updateById", "UPDATE", "sys_role",
                "UPDATE sys_role SET name = ? WHERE id = ?", 20, 1, "trace-role"));

        var logs = service.recent(new SqlLogQuery(10, null, "user", true));

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).statementId()).isEqualTo("UserMapper.selectPage");
        assertThat(logs.get(0).slow()).isTrue();
    }
}
