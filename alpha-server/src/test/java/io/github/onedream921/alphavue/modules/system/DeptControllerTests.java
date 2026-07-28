package io.github.onedream921.alphavue.modules.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeptControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeFixtures() {
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'dept-delete-test-%'");
        jdbcTemplate.update("DELETE FROM sys_dept WHERE name LIKE 'dept-delete-test-%'");
    }

    @Test
    void rejectsDeletingDepartmentWithChildrenUsingActionableMessage() throws Exception {
        long parentId = insertDept("dept-delete-test-parent", 0);
        insertDept("dept-delete-test-child", parentId);

        mockMvc.perform(delete("/api/system/depts/{id}", parentId).header("Authorization", bearer(loginAdmin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先删除该部门下的子部门"));
    }

    @Test
    void rejectsDeletingDepartmentWithUsersUsingActionableMessage() throws Exception {
        long deptId = insertDept("dept-delete-test-user", 0);
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, dept_id, must_change_password) "
                        + "VALUES (?, ?, ?, ?, 0)",
                "dept-delete-test-user", BCrypt.hashpw("password-123", BCrypt.gensalt()), "fixture", deptId);

        mockMvc.perform(delete("/api/system/depts/{id}", deptId).header("Authorization", bearer(loginAdmin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先将该部门下的用户调整到其他部门"));
    }

    private long insertDept(String name, long parentId) {
        jdbcTemplate.update("INSERT INTO sys_dept (parent_id, name, sort_order, status) VALUES (?, ?, 0, 1)",
                parentId, name);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_dept WHERE name = ?", Long.class, name);
    }

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
