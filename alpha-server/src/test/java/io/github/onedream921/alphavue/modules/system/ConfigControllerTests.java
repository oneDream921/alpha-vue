package io.github.onedream921.alphavue.modules.system;

import io.github.onedream921.alphavue.modules.system.service.ConfigCacheStore;
import io.github.onedream921.alphavue.modules.system.service.ConfigService;
import io.github.onedream921.alphavue.modules.system.dto.ConfigRequests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ConfigCacheStore configCacheStore;
    @Autowired private ConfigService configService;
    @Autowired private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key LIKE 'file.%'");
        jdbcTemplate.update("DELETE FROM sys_config_definition WHERE config_key LIKE 'file.test-%' OR config_key IN ('file.notice', 'file.internal-note', 'file.unsafe')");
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE module = 'System' AND operation LIKE '%configuration'");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = 'config-viewer'");
        configCacheStore.evict("file.upload.max-size-mb");
        configCacheStore.evict("file.upload.allowed-extensions");
        configCacheStore.evict("file.private-access-ttl-minutes");
    }

    @Test
    void onlyRegisteredKeysAndValidValuesCanBeSaved() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("file.upload.max-size-mb", "100", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.dataType").value("INTEGER"));
        assertThat(configCacheStore.get("file.upload.max-size-mb")).isEqualTo("100");
        for (String invalid : new String[] {"0", "101", "not-a-number"}) {
            mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                            .content(body("file.upload.max-size-mb", invalid, true)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数错误"));
        }
        mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("spring.redis.host", "x", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesStringBoundariesAndPublishesOnlyAfterSuccessfulWrite() throws Exception {
        String token = login();
        MvcResult result = mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("file.upload.allowed-extensions", "jpg,png,pdf", true)))
                .andExpect(status().isOk()).andReturn();
        long id = id(result);
        mockMvc.perform(put("/api/system/configs/{id}", id).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("file.upload.allowed-extensions", "JPG", true)))
                .andExpect(status().isBadRequest());
        assertThat(configCacheStore.get("file.upload.allowed-extensions")).isEqualTo("jpg,png,pdf");
    }

    @Test
    void deniesUnauthenticatedWrites() throws Exception {
        mockMvc.perform(post("/api/system/configs").contentType(MediaType.APPLICATION_JSON)
                        .content(body("file.private-access-ttl-minutes", "15", true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managesOnlyControlledFileDefinitionsAndRedactsSensitiveDefaults() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody("file.notice", "STRING", "public", false, false, null, "PUBLISHED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.configKey").value("file.notice"));
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody("file.internal-note", "STRING", "hidden-value", true, false, null, "PUBLISHED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.defaultValue").doesNotExist());
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody("file.unsafe", "STRING", "x", true, true, "FILE_UPLOAD_MAX_SIZE", "PUBLISHED")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void validatesAllValueTypesAndRejectsMalformedDefinitionRules() throws Exception {
        String token = login();
        createDefinition(token, definitionBody("file.test-boolean", "BOOLEAN", "true", false, false, null, "PUBLISHED"));
        createDefinition(token, definitionBody("file.test-integer", "INTEGER", "3", false, false, null, "PUBLISHED")
                .replace("\"status\"", "\"integerMin\":1,\"integerMax\":3,\"status\""));
        createDefinition(token, definitionBody("file.test-enum", "ENUM", "alpha", false, false, null, "PUBLISHED")
                .replace("\"status\"", "\"enumValues\":\"alpha,beta\",\"status\""));
        createDefinition(token, definitionBody("file.test-string", "STRING", "abc", false, false, null, "PUBLISHED")
                .replace("\"status\"", "\"stringMaxLength\":3,\"stringPattern\":\"[a-z]+\",\"status\""));

        for (String[] pair : new String[][] {{"file.test-boolean", "true"}, {"file.test-integer", "3"}, {"file.test-enum", "beta"}, {"file.test-string", "abc"}}) {
            mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                            .content(body(pair[0], pair[1], true)))
                    .andExpect(status().isOk());
        }
        for (String[] pair : new String[][] {{"file.test-boolean", "TRUE"}, {"file.test-integer", "4"}, {"file.test-enum", "gamma"}, {"file.test-string", "ABCD"}}) {
            mockMvc.perform(put("/api/system/configs/{id}", idFor(pair[0])).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                            .content(body(pair[0], pair[1], true)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数错误"));
        }
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(definitionBody("file.test-pattern", "STRING", "x", false, false, null, "DRAFT")
                                .replace("\"status\"", "\"stringPattern\":\"(\",\"status\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void protectsDynamicDefinitionsAndRejectsDuplicateRuntimeBindings() throws Exception {
        String token = login();
        long id = jdbcTemplate.queryForObject("SELECT id FROM sys_config_definition WHERE config_key = 'file.upload.max-size-mb'", Long.class);
        mockMvc.perform(put("/api/system/configs/definitions/{id}", id).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(definitionBody("file.upload.max-size-mb", "INTEGER", "10", false, true, "FILE_UPLOAD_MAX_SIZE", "DISABLED")
                                .replace("\"status\"", "\"integerMin\":1,\"integerMax\":100,\"status\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数错误"));
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(definitionBody("file.test-duplicate", "INTEGER", "1", false, true, "FILE_UPLOAD_MAX_SIZE", "PUBLISHED")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void deniesDefinitionWritesWithoutPermissionAndKeepsSensitiveValuesOutOfResponsesAndAudit() throws Exception {
        String token = login();
        String viewerToken = loginWithoutConfigPermissions();
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody("file.test-no-access", "STRING", "x", false, false, null, "DRAFT")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("没有操作权限"));
        mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body("file.upload.max-size-mb", "10", true)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("没有操作权限"));
        createDefinition(token, definitionBody("file.test-sensitive", "STRING", "private-default", true, false, null, "PUBLISHED"));
        mockMvc.perform(post("/api/system/configs").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("file.test-sensitive", "private-value", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.configValue").doesNotExist());
        waitForAudit();
        assertThat(jdbcTemplate.queryForList("SELECT request_params FROM sys_oper_log WHERE operation LIKE '%configuration'", String.class))
                .allMatch("[redacted]"::equals);
    }

    @Test
    void publishesCacheOnlyAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            configService.create(new ConfigRequests.Save("file.upload.max-size-mb", "22", true, null));
            status.setRollbackOnly();
        });
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_config WHERE config_key = 'file.upload.max-size-mb'", Integer.class)).isZero();
        assertThat(configCacheStore.get("file.upload.max-size-mb")).isNull();
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString().replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
    private static String bearer(String token) { return "Bearer " + token; }
    private static String body(String key, String value, boolean enabled) {
        return "{\"configKey\":\"" + key + "\",\"configValue\":\"" + value + "\",\"enabled\":" + enabled + "}";
    }
    private static long id(MvcResult result) throws Exception {
        return Long.parseLong(result.getResponse().getContentAsString().replaceFirst("(?s).*\\\"id\\\"\\s*:\\s*\\\"?(\\d+)\\\"?.*", "$1"));
    }
    private long idFor(String key) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_config WHERE config_key = ?", Long.class, key);
    }
    private void createDefinition(String token, String value) throws Exception {
        mockMvc.perform(post("/api/system/configs/definitions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(value))
                .andExpect(status().isOk());
    }
    private String loginWithoutConfigPermissions() throws Exception {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password, status, deleted) VALUES ('config-viewer', ?, '配置查看者', 0, 1, 0)",
                "$2a$10$v6eFc6AgyU7o6oIjdA/V1eJctWdbQX9ydbfXfQd0JMht/trbUgurO");
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"config-viewer\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString().replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
    private void waitForAudit() throws InterruptedException {
        for (int attempt = 0; attempt < 25; attempt++) {
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_oper_log WHERE operation LIKE '%configuration'", Integer.class) > 0) return;
            Thread.sleep(20);
        }
    }
    private static String definitionBody(String key, String type, String defaultValue, boolean sensitive, boolean dynamic,
                                         String binding, String status) {
        return "{\"configKey\":\"" + key + "\",\"configName\":\"测试定义\",\"valueType\":\"" + type
                + "\",\"defaultValue\":\"" + defaultValue + "\",\"sensitive\":" + sensitive
                + ",\"dynamic\":" + dynamic + ",\"runtimeBinding\":" + (binding == null ? "null" : "\"" + binding + "\"")
                + ",\"status\":\"" + status + "\"}";
    }
}
