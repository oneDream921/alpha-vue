package io.github.onedream921.alphavue.modules.system;

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
import io.github.onedream921.alphavue.modules.system.service.ConfigCacheStore;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigCacheStore configCacheStore;

    @BeforeEach
    void removeConfigFixtures() {
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key LIKE 'config-test.%'");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code = 'CONFIG_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'config-list-only') "
                + "OR role_id IN (SELECT id FROM sys_role WHERE code = 'CONFIG_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = 'config-list-only'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code = 'CONFIG_LIST_ONLY'");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE title = 'Config list fixture'");
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE module = 'System' AND operation LIKE '%configuration'");
    }

    @Test
    void allowsCrudForSuperAdminAndKeepsValuesOutOfOperationLogs() throws Exception {
        String token = login("admin");
        String value = "value-must-not-appear-in-audit-log";

        MvcResult created = mockMvc.perform(post("/api/system/configs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody("首页公告", "config-test.notice", value, "portal", "STRING", true, "首页公告")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configName").value("首页公告"))
                .andExpect(jsonPath("$.data.configKey").value("config-test.notice"))
                .andExpect(jsonPath("$.data.configValue").value(value))
                .andExpect(jsonPath("$.data.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
                .andReturn();
        long id = jsonId(created);
        assertThat(configCacheStore.get("config-test.notice")).isEqualTo(value);

        mockMvc.perform(get("/api/system/configs?page=1&size=10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.configKey == 'config-test.notice')]").exists());
        mockMvc.perform(get("/api/system/configs/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("首页公告"));
        mockMvc.perform(put("/api/system/configs/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody("首页公告", "config-test.notice-v2", "updated-value", "portal", "STRING", true, "更新公告")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configKey").value("config-test.notice-v2"))
                .andExpect(jsonPath("$.data.configValue").value("updated-value"));
        assertThat(configCacheStore.get("config-test.notice")).isNull();
        assertThat(configCacheStore.get("config-test.notice-v2")).isEqualTo("updated-value");
        mockMvc.perform(delete("/api/system/configs/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertThat(configCacheStore.get("config-test.notice-v2")).isNull();

        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM sys_config WHERE id = ?", Long.class, id))
                .isEqualTo(id);
        awaitRedactedAudit("Create configuration");
        assertThat(jdbcTemplate.queryForList("SELECT request_params FROM sys_oper_log WHERE module = 'System' "
                + "AND operation LIKE '%configuration'", String.class)).doesNotContain(value, "updated-value");
    }

    @Test
    void rejectsDuplicateOrSensitiveConfigurationKeys() throws Exception {
        String token = login("admin");

        mockMvc.perform(post("/api/system/configs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody("唯一参数", "config-test.unique", "first", "general", "STRING", true, null)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/system/configs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody("唯一参数", "config-test.unique", "second", "general", "STRING", true, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数错误"));
        for (String prefix : List.of("spring.", "server.", "datasource.", "redis.", "minio.", "sa-token.")) {
            mockMvc.perform(post("/api/system/configs")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(configBody("受限参数", prefix + "managed", "forbidden", "general", "STRING", true, null)))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/system/configs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody("受限参数", "config-test.api-token", "forbidden", "general", "STRING", true, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enforcesListAndWritePermissionsIndependently() throws Exception {
        long userId = insertUser();
        long roleId = insertRole();
        long menuId = insertListMenu();
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        String token = login("config-list-only");

        mockMvc.perform(get("/api/system/configs").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/system/configs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody("无权参数", "config-test.denied", "blocked", "general", "STRING", true, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        awaitRedactedAudit("Create configuration");
    }

    private long insertUser() {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password) VALUES (?, ?, ?, 0)",
                "config-list-only", BCrypt.hashpw("password-123", BCrypt.gensalt()), "config-list-only");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'config-list-only'", Long.class);
    }

    private long insertRole() {
        jdbcTemplate.update("INSERT INTO sys_role (name, code) VALUES ('Config list only', 'CONFIG_LIST_ONLY')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE code = 'CONFIG_LIST_ONLY'", Long.class);
    }

    private long insertListMenu() {
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, title, menu_type, permission) VALUES (0, 'Config list fixture', 'BUTTON', 'system:config:list')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE title = 'Config list fixture'", Long.class);
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

    private static long jsonId(MvcResult result) throws Exception {
        return Long.parseLong(result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"id\\\"\\s*:\\s*\\\"?(\\d+)\\\"?.*", "$1"));
    }

    private static String configBody(String configName, String configKey, String configValue, String configGroup,
                                     String dataType, boolean enabled, String description) {
        String body = "{\"configName\":\"" + configName + "\",\"configKey\":\"" + configKey
                + "\",\"configValue\":\"" + configValue + "\",\"configGroup\":\"" + configGroup
                + "\",\"dataType\":\"" + dataType + "\",\"enabled\":" + enabled;
        return description == null ? body + "}" : body + ",\"description\":\"" + description + "\"}";
    }

    private void awaitRedactedAudit(String operation) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            List<String> requestParameters = jdbcTemplate.queryForList("SELECT request_params FROM sys_oper_log "
                    + "WHERE module = 'System' AND operation = ?", String.class, operation);
            if (requestParameters.contains("[redacted]")) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for redacted audit of " + operation);
    }
}
