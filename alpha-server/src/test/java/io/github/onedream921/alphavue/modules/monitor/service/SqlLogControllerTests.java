package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.framework.mybatis.SqlLogCapture;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogSettingsRequest;
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

import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        sqlLogService.updateSettings(new SqlLogSettingsRequest(true, Set.of()));
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code = 'SQL_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'sql-list-only') "
                + "OR role_id IN (SELECT id FROM sys_role WHERE code = 'SQL_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = 'sql-list-only'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code = 'SQL_LIST_ONLY'");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE title = 'SQL list fixture'");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE title = 'SQL control fixture'");
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
    void enforcesClearPermissionAndRejectsRemovedDruidEndpoint() throws Exception {
        long userId = insertUser();
        long roleId = insertRole();
        long menuId = insertListMenu();
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        String listOnlyToken = login("sql-list-only");

        mockMvc.perform(get("/api/monitor/sql/druid-url")
                        .header("Authorization", bearer(listOnlyToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/monitor/sql/logs")
                        .header("Authorization", bearer(listOnlyToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(delete("/api/monitor/sql/logs")
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SQL 日志已清空"));
    }

    @Test
    void exposesAndControlsRuntimeCollectionSettings() throws Exception {
        sqlLogService.record(new SqlLogCapture("UserMapper.selectPage", "SELECT", "sys_user",
                "SELECT * FROM sys_user", 12, 1, "trace-user"));

        mockMvc.perform(get("/api/monitor/sql/settings")
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.statements[*].statementId").value(hasItem("UserMapper.selectPage")));

        mockMvc.perform(put("/api/monitor/sql/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"excludedStatementIds\":[\"UserMapper.selectPage\"]}")
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.excludedStatementIds[0]").value("UserMapper.selectPage"));

        sqlLogService.clear();
        sqlLogService.record(new SqlLogCapture("RoleMapper.selectPage", "SELECT", "sys_role",
                "SELECT * FROM sys_role", 12, 1, "trace-role"));
        mockMvc.perform(get("/api/monitor/sql/logs")
                        .param("limit", "10")
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void requiresControlPermissionToUpdateSettings() throws Exception {
        long userId = insertUser();
        long roleId = insertRole();
        long listMenuId = insertListMenu();
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, listMenuId);

        mockMvc.perform(put("/api/monitor/sql/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"excludedStatementIds\":[]}")
                        .header("Authorization", bearer(login("sql-list-only"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
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
                                + ("admin".equals(username) ? "admin123" : "password-123") + "\",\"clientId\":\"pc-admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString().replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
