package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.framework.mybatis.SqlLogCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SQL 日志接口测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SqlLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlLogService sqlLogService;

    @BeforeEach
    void removeFixtures() {
        sqlLogService.clear();
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code = 'SQL_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'sql-list-only') "
                + "OR role_id IN (SELECT id FROM sys_role WHERE code = 'SQL_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = 'sql-list-only'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code = 'SQL_LIST_ONLY'");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE title = 'SQL list fixture'");
    }

    @Test
    void returnsRecentSqlLogsWithoutParameterValues() throws Exception {
        sqlLogService.record(new SqlLogCapture("UserMapper.selectByUsername", "SELECT", "sys_user",
                "SELECT id, username FROM sys_user WHERE username = ?", 600, 1, "trace-sql"));

        mockMvc.perform(get("/api/monitor/sql/logs")
                        .param("limit", "10")
                        .param("keyword", "sys_user")
                        .param("slowOnly", "true")
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].statementId").value("UserMapper.selectByUsername"))
                .andExpect(jsonPath("$.data[0].slow").value(true))
                .andExpect(jsonPath("$.data[0].sql").value("SELECT id, username FROM sys_user WHERE username = ?"))
                .andExpect(content().string(not(containsString("admin123"))));
    }

    @Test
    void enforcesClearPermissionAndReturnsDruidInfo() throws Exception {
        long userId = insertUser();
        long roleId = insertRole();
        long menuId = insertListMenu();
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        String listOnlyToken = login("sql-list-only");

        mockMvc.perform(get("/api/monitor/sql/druid-url")
                        .header("Authorization", bearer(listOnlyToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.path").value("/druid/index.html"));

        mockMvc.perform(delete("/api/monitor/sql/logs")
                        .header("Authorization", bearer(listOnlyToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(delete("/api/monitor/sql/logs")
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SQL 日志已清空"));
    }

    private long insertUser() {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password) VALUES (?, ?, ?, 0)",
                "sql-list-only", BCrypt.hashpw("password-123", BCrypt.gensalt()), "sql-list-only");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'sql-list-only'", Long.class);
    }

    private long insertRole() {
        jdbcTemplate.update("INSERT INTO sys_role (name, code) VALUES ('SQL list only', 'SQL_LIST_ONLY')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE code = 'SQL_LIST_ONLY'", Long.class);
    }

    private long insertListMenu() {
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, title, menu_type, permission) VALUES (0, 'SQL list fixture', 'BUTTON', 'monitor:sql:list')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE title = 'SQL list fixture'", Long.class);
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\""
                                + ("admin".equals(username) ? "admin123" : "password-123") + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString().replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
