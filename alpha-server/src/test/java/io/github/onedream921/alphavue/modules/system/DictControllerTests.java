package io.github.onedream921.alphavue.modules.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DictControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeDictionaryFixtures() {
        jdbcTemplate.update("DELETE FROM sys_dict_item WHERE type_id IN (SELECT id FROM sys_dict_type WHERE type_code LIKE 'dict-test.%')");
        jdbcTemplate.update("DELETE FROM sys_dict_type WHERE type_code LIKE 'dict-test.%'");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code = 'DICT_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'dict-list-only') "
                + "OR role_id IN (SELECT id FROM sys_role WHERE code = 'DICT_LIST_ONLY')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = 'dict-list-only'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code = 'DICT_LIST_ONLY'");
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE module = 'System' AND operation LIKE '%dictionary%'");
    }

    @Test
    void superAdminCanCreateDictionaryType() throws Exception {
        String token = login("admin");

        mockMvc.perform(post("/api/system/dict-types")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"dict-test.status\",\"typeName\":\"状态\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeCode").value("dict-test.status"));
    }

    @Test
    void managesDictionaryTypesAndEnforcesTypeCodeRules() throws Exception {
        String token = login("admin");
        long id = createType(token, "dict-test.status", "状态");

        mockMvc.perform(get("/api/system/dict-types?page=1&size=10").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(id))
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/system/dict-types/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeName").value("状态"));
        mockMvc.perform(put("/api/system/dict-types/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"dict-test.status\",\"typeName\":\"业务状态\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeName").value("业务状态"));
        mockMvc.perform(put("/api/system/dict-types/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"dict-test.changed\",\"typeName\":\"业务状态\",\"status\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("字典类型编码不可修改"));
        mockMvc.perform(post("/api/system/dict-types")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"dict-test.status\",\"typeName\":\"重复\",\"status\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("字典类型编码已存在"));
        mockMvc.perform(delete("/api/system/dict-types/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM sys_dict_type WHERE id = ?", Long.class, id))
                .isEqualTo(id);
        createType(token, "dict-test.status", "重新创建");
    }

    @Test
    void listPermissionDoesNotGrantDictionaryWrites() throws Exception {
        long userId = insertListOnlyUser();
        long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE code = 'DICT_LIST_ONLY'", Long.class);
        long menuId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission = 'system:dict:list'", Long.class);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        String token = login("dict-list-only", "password-123");

        mockMvc.perform(get("/api/system/dict-types").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/system/dict-types")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"dict-test.denied\",\"typeName\":\"拒绝\",\"status\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(put("/api/system/dicts/cache").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void managesItemsAndReturnsOnlyEnabledItemsToAuthenticatedReaders() throws Exception {
        String token = login("admin");
        long typeId = createType(token, "dict-test.workflow", "流程状态");
        long draftId = createItem(token, typeId, "草稿", "draft", 20, 1, 1);
        createItem(token, typeId, "启用", "enabled", 10, 1, 0);
        createItem(token, typeId, "停用", "disabled", 30, 0, 0);

        mockMvc.perform(get("/api/system/dict-types/{typeId}/items?page=1&size=10", typeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].value").value("enabled"))
                .andExpect(jsonPath("$.data.total").value(3));
        mockMvc.perform(put("/api/system/dict-items/{id}", draftId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"草稿中\",\"value\":\"draft\",\"sortOrder\":5,\"status\":1,\"isDefault\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("草稿中"));
        mockMvc.perform(post("/api/system/dict-types/{typeId}/items", typeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"重复\",\"value\":\"draft\",\"sortOrder\":0,\"status\":1,\"isDefault\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("字典项值已存在"));
        mockMvc.perform(get("/api/system/dicts/{typeCode}/items", "dict-test.workflow")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value("draft"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].remark").doesNotExist())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/system/dicts/{typeCode}/items", "dict-test.workflow"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/system/dicts/cache")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeCount").isNumber());
        mockMvc.perform(delete("/api/system/dict-types/{id}", typeId).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先删除该字典类型下的字典项"));
        mockMvc.perform(delete("/api/system/dict-items/{id}", draftId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        createItem(token, typeId, "草稿重新创建", "draft", 5, 1, 0);
    }

    @Test
    void dictionaryWriteAuditLogsAreRedacted() throws Exception {
        String token = login("admin");
        String remark = "dict-test-audit-must-not-appear";
        mockMvc.perform(post("/api/system/dict-types")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"dict-test.audit\",\"typeName\":\"审计字典\",\"status\":1,\"remark\":\"" + remark + "\"}"))
                .andExpect(status().isOk());

        awaitRedactedAudit("Create dictionary type");
        assertThat(jdbcTemplate.queryForList("SELECT request_params FROM sys_oper_log WHERE module = 'System' "
                + "AND operation = 'Create dictionary type'", String.class)).doesNotContain(remark, "审计字典");
    }

    private long createType(String token, String typeCode, String typeName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/system/dict-types")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeCode\":\"" + typeCode + "\",\"typeName\":\"" + typeName
                                + "\",\"status\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        return Long.parseLong(result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"id\\\"\\s*:\\s*\\\"?(\\d+)\\\"?.*", "$1"));
    }

    private long createItem(String token, long typeId, String label, String value, int sortOrder, int status,
                            int isDefault) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/system/dict-types/{typeId}/items", typeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"" + label + "\",\"value\":\"" + value
                                + "\",\"sortOrder\":" + sortOrder + ",\"status\":" + status
                                + ",\"isDefault\":" + isDefault + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return Long.parseLong(result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"id\\\"\\s*:\\s*\\\"?(\\d+)\\\"?.*", "$1"));
    }

    private long insertListOnlyUser() {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password) VALUES (?, ?, ?, 0)",
                "dict-list-only", org.mindrot.jbcrypt.BCrypt.hashpw("password-123", org.mindrot.jbcrypt.BCrypt.gensalt()),
                "dict-list-only");
        jdbcTemplate.update("INSERT INTO sys_role (name, code) VALUES ('Dict list only', 'DICT_LIST_ONLY')");
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'dict-list-only'", Long.class);
    }

    private String login(String username) throws Exception {
        return login(username, "admin123");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private void awaitRedactedAudit(String operation) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            List<String> requestParameters = jdbcTemplate.queryForList("SELECT request_params FROM sys_oper_log "
                    + "WHERE module = 'System' AND operation = ?", String.class, operation);
            if (requestParameters.contains("[redacted]")) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for redacted dictionary audit");
    }
}
