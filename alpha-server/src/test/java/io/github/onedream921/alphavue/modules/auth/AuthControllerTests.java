package io.github.onedream921.alphavue.modules.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrect\",\"clientId\":\"pc-admin\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void rejectsMissingOrUnknownClientId() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"clientId\":\"unknown\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsProfileRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Frame-Options", "DENY"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", "no-store"));
    }

    @Test
    void logsInAndRetrievesProtectedProfileWithBearerToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"clientId\":\"pc-admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String token = login.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void replacesOnlyThePreviousSessionForTheSameClient() throws Exception {
        String firstToken = login("admin", "admin123", "pc-admin");
        String secondToken = login("admin", "admin123", "pc-admin");

        mockMvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void keepsSessionsForDifferentClients() throws Exception {
        jdbcTemplate.update("INSERT INTO sys_client (client_id, name) VALUES ('mobile-app', '移动端')");
        String pcToken = login("admin", "admin123", "pc-admin");
        String mobileToken = login("admin", "admin123", "mobile-app");

        mockMvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + pcToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + mobileToken))
                .andExpect(status().isOk());
    }

    @Test
    void distinguishesIncorrectCurrentPasswordFromReusingTheCurrentPassword() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"clientId\":\"pc-admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = login.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"incorrect-old\",\"newPassword\":\"different-new\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("旧密码错误"));

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"admin123\",\"newPassword\":\"admin123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("新密码不能与旧密码相同"));
    }

    @Test
    void admitsOnlyFiveSimultaneousFailedLoginAttemptsPerUsernameAndIp() throws Exception {
        int attempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int index = 0; index < attempts; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                    return mockMvc.perform(post("/api/auth/login")
                                    .with(request -> {
                                        request.setRemoteAddr("198.51.100.23");
                                        return request;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"username\":\"admin\",\"password\":\"incorrect\",\"clientId\":\"pc-admin\"}"))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).filteredOn(status -> status == 401).hasSize(5);
            assertThat(statuses).filteredOn(status -> status == 429).hasSize(5);
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.23");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"incorrect\",\"clientId\":\"pc-admin\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value(429));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void recordsLogoutOperationWithThePrincipalThatInitiatedIt() throws Exception {
        jdbcTemplate.update("DELETE FROM sys_oper_log");
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"clientId\":\"pc-admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = login.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        OperationAudit audit = awaitLogoutAudit();
        assertThat(audit.userId()).isEqualTo(1L);
        assertThat(audit.username()).isEqualTo("admin");
    }

    private OperationAudit awaitLogoutAudit() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            List<OperationAudit> audits = jdbcTemplate.query("""
                            SELECT user_id, username
                            FROM sys_oper_log
                            WHERE operation = 'Logout'
                            ORDER BY id DESC
                            LIMIT 1
                            """, (resultSet, rowNum) -> new OperationAudit(
                    resultSet.getObject("user_id", Long.class), resultSet.getString("username")));
            if (!audits.isEmpty()) {
                return audits.getFirst();
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for the logout operation audit");
    }

    private record OperationAudit(Long userId, String username) {
    }

    private String login(String username, String password, String clientId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password
                                + "\",\"clientId\":\"" + clientId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
}
