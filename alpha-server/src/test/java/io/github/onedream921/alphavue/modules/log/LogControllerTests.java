package io.github.onedream921.alphavue.modules.log;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Test
    void listsOperationAndLoginLogsForAdministrator() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/logs/operations?page=1&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isArray());

        mockMvc.perform(get("/api/logs/logins?page=1&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records[?(@.username == 'admin')]").exists());
    }

    @Test
    void returnsBadRequestForInvalidPageParameter() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/logs/operations?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数校验失败"));
    }

    @Test
    void exposesStoredExceptionStackToAuthorizedOperationLogReaders() throws Exception {
        SysOperLog log = new SysOperLog();
        log.setUsername("exception-stack-test");
        log.setModule("Test");
        log.setOperation("Store exception stack");
        log.setBusinessType("UPDATE");
        log.setMethod("PUT");
        log.setRequestUri("/api/test/exception-stack");
        log.setRequestParams("[redacted]");
        log.setResponseCode(500);
        log.setStatus(0);
        log.setIpAddress("127.0.0.1");
        log.setDurationMs(1L);
        log.setTraceId("exception-stack-trace");
        log.setExceptionStack("java.lang.IllegalStateException: test failure\\n\\tat test.Stack.trace(Stack.java:1)");
        log.setHandled(0);
        log.setHandlingStatus(0);
        operLogMapper.insert(log);

        String token = login();

        mockMvc.perform(get("/api/logs/operations?page=1&size=10&keyword=exception-stack-test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].exceptionStack")
                        .value("java.lang.IllegalStateException: test failure\\n\\tat test.Stack.trace(Stack.java:1)"));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
}
