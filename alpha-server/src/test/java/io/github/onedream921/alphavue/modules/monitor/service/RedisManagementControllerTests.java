package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyMetadataVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Redis 运维台接口测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RedisManagementControllerTests.FakeRedisKeyspaceConfiguration.class)
class RedisManagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeFixtures() {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code = 'REDIS_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'redis-list-only') "
                + "OR role_id IN (SELECT id FROM sys_role WHERE code = 'REDIS_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = 'redis-list-only'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code = 'REDIS_LIST_ONLY'");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE title = 'Redis list fixture'");
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE module = 'Monitor' AND operation = '删除 Redis 键'");
    }

    @Test
    void listsOnlyManagedPrefixWithCursorAndRedactsCaptchaValues() throws Exception {
        String token = login("admin");

        mockMvc.perform(get("/api/monitor/redis/keys")
                        .param("prefix", "auth:")
                        .param("cursor", "0")
                        .param("count", "1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].key").value("auth:captcha:challenge-1"))
                .andExpect(jsonPath("$.data.records[0].category").value("验证码"))
                .andExpect(jsonPath("$.data.records[0].type").value("string"))
                .andExpect(jsonPath("$.data.records[0].ttlSeconds").value(120))
                .andExpect(jsonPath("$.data.records[0].valueRedacted").value(true))
                .andExpect(jsonPath("$.data.records[0].value").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").value("1"))
                .andExpect(jsonPath("$.data.hasMore").value(true));
    }

    @Test
    void rejectsOutOfScopePrefixesAndDirectKeys() throws Exception {
        String token = login("admin");

        mockMvc.perform(get("/api/monitor/redis/keys")
                        .param("prefix", "other-app:")
                        .param("cursor", "0")
                        .param("count", "10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数错误"));
        mockMvc.perform(get("/api/monitor/redis/key")
                        .param("key", "other-app:secret")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数错误"));
        mockMvc.perform(get("/api/monitor/redis/keys")
                        .param("prefix", "auth:")
                        .param("cursor", "0")
                        .param("count", "101")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数校验失败"));
    }

    @Test
    void enforcesDeletePermissionAndKeepsKeyOutOfResponseAndAudit() throws Exception {
        long userId = insertUser();
        long roleId = insertRole();
        long menuId = insertListMenu();
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        String listOnlyToken = login("redis-list-only");
        String managedKey = "auth:captcha:challenge-1";

        mockMvc.perform(delete("/api/monitor/redis/key")
                        .param("key", managedKey)
                        .header("Authorization", bearer(listOnlyToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(delete("/api/monitor/redis/key")
                        .param("key", managedKey)
                        .header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Redis 键已删除"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(managedKey))));

        awaitRedactedAudit();
    }

    @Test
    void returnsSelectedOverviewWithoutRawInfoPayload() throws Exception {
        mockMvc.perform(get("/api/monitor/redis/overview").header("Authorization", bearer(login("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.redisVersion").value("7.4.1"))
                .andExpect(jsonPath("$.data.usedMemoryBytes").value(4096))
                .andExpect(jsonPath("$.data.managedKeyCounts.auth:").value(2))
                .andExpect(jsonPath("$.data.info").doesNotExist());
    }

    private long insertUser() {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password) VALUES (?, ?, ?, 0)",
                "redis-list-only", BCrypt.hashpw("password-123", BCrypt.gensalt()), "redis-list-only");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'redis-list-only'", Long.class);
    }

    private long insertRole() {
        jdbcTemplate.update("INSERT INTO sys_role (name, code) VALUES ('Redis list only', 'REDIS_LIST_ONLY')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE code = 'REDIS_LIST_ONLY'", Long.class);
    }

    private long insertListMenu() {
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, title, menu_type, permission) VALUES (0, 'Redis list fixture', 'BUTTON', 'monitor:redis:list')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE title = 'Redis list fixture'", Long.class);
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

    private void awaitRedactedAudit() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            List<String> requestParameters = jdbcTemplate.queryForList("SELECT request_params FROM sys_oper_log "
                    + "WHERE module = 'Monitor' AND operation = '删除 Redis 键'", String.class);
            if (requestParameters.contains("[redacted]")) {
                assertThat(requestParameters).doesNotContain("auth:captcha:challenge-1");
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for redacted Redis deletion audit");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeRedisKeyspaceConfiguration {
        @Bean
        @Primary
        RedisKeyspace redisKeyspace() {
            return new InMemoryRedisKeyspace();
        }
    }

    private static final class InMemoryRedisKeyspace implements RedisKeyspace {
        private final Map<String, RedisKeyMetadata> entries = new LinkedHashMap<>();

        private InMemoryRedisKeyspace() {
            entries.put("auth:captcha:challenge-1", new RedisKeyMetadata("auth:captcha:challenge-1", "string", 120L, 24L));
            entries.put("auth:login:failure:alice:127.0.0.1", new RedisKeyMetadata("auth:login:failure:alice:127.0.0.1", "string", 30L, 48L));
            entries.put("satoken:login:1", new RedisKeyMetadata("satoken:login:1", "string", 1800L, 128L));
            entries.put("other-app:secret", new RedisKeyMetadata("other-app:secret", "string", 3600L, 128L));
        }

        @Override
        public RedisScanResult scan(String prefix, String cursor, int count) {
            List<RedisKeyMetadata> matching = entries.values().stream()
                    .filter(entry -> entry.key().startsWith(prefix))
                    .toList();
            int offset = Integer.parseInt(cursor);
            int end = Math.min(offset + count, matching.size());
            String nextCursor = end == matching.size() ? "0" : Integer.toString(end);
            return new RedisScanResult(matching.subList(offset, end), nextCursor);
        }

        @Override
        public RedisKeyMetadata metadata(String key) {
            return entries.get(key);
        }

        @Override
        public boolean delete(String key) {
            return entries.remove(key) != null;
        }

        @Override
        public RedisOverview overview() {
            return new RedisOverview("7.4.1", 123L, 4096L, 3L, Map.of("auth:", 2L, "satoken:", 1L));
        }
    }
}
