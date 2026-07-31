package io.github.onedream921.alphavue.modules.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnlineSessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listsSessionsAndKicksOutOnlyTheSelectedTerminal() throws Exception {
        long userId = insertUser("online-session-user");
        String userToken = login("online-session-user", "admin123");
        String adminToken = login("admin", "admin123");

        MvcResult result = mockMvc.perform(get("/api/monitor/online-users?page=1&size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("records");
        JsonNode target = null;
        for (JsonNode record : records) {
            if ("online-session-user".equals(record.path("user").path("username").asText())) {
                target = record;
                break;
            }
        }
        assertThat(target).isNotNull();
        assertThat(target.path("ipAddress").asText()).isNotBlank();
        assertThat(target.path("browser").asText()).isEqualTo("Chrome");
        assertThat(target.path("operatingSystem").asText()).isEqualTo("macOS");
        int terminalIndex = target.path("terminalIndex").asInt();

        mockMvc.perform(delete("/api/monitor/online-users/{userId}/sessions/{terminalIndex}", userId, terminalIndex)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/profile").header("Authorization", bearer(userToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportsTheActualTotalWhenRequestedPageIsPastTheEnd() throws Exception {
        String adminToken = login("admin", "admin123");

        MvcResult firstPage = mockMvc.perform(get("/api/monitor/online-users?page=1&size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        long firstPageTotal = objectMapper.readTree(firstPage.getResponse().getContentAsString())
                .path("data").path("total").asLong();

        MvcResult result = mockMvc.perform(get("/api/monitor/online-users?page=2&size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(firstPageTotal);
        assertThat(data.path("records")).isEmpty();
    }

    private long insertUser(String username) {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password, status, deleted) VALUES (?, ?, ?, 0, 1, 0)",
                username, "$2a$10$v6eFc6AgyU7o6oIjdA/V1eJctWdbQX9ydbfXfQd0JMht/trbUgurO", username);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password
                                + "\",\"clientId\":\"pc-admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
