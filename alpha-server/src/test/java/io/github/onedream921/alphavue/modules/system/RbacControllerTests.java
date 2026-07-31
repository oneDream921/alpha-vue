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

import java.util.List;
import java.util.concurrent.TimeUnit;

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
class RbacControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeRbacFixtures() {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE code LIKE 'RBAC_%')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'rbac-%') "
                + "OR role_id IN (SELECT id FROM sys_role WHERE code LIKE 'RBAC_%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'rbac-%'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code LIKE 'RBAC_%'");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE title LIKE 'RBAC %'");
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE module = 'System'");
    }

    @Test
    void deniesUserManagementToAuthenticatedUserWithoutPermission() throws Exception {
        long userId = insertUser("rbac-no-access");
        long roleId = insertRole("RBAC_NO_ACCESS");
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);

        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(login("rbac-no-access"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(login("rbac-no-access")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rbac-denied-create\",\"password\":\"change-me-123\",\"nickname\":\"Denied\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        awaitFailedOperationAudit("Create user", 403);
    }

    @Test
    void listsUsersForUserGrantedListPermission() throws Exception {
        long userId = insertUser("rbac-list-user");
        long roleId = insertRole("RBAC_LIST_USER");
        long menuId = insertMenu("RBAC User List", "system:user:list");
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);

        mockMvc.perform(get("/api/system/users?page=1&size=10")
                        .header("Authorization", bearer(login("rbac-list-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records[?(@.username == 'admin')]").exists());
    }

    @Test
    void treatsAdminAsBuiltInSuperUserAndRejectsRoleChanges() throws Exception {
        long adminId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", adminId);
        long roleId = insertRole("RBAC_ADMIN_ROLE_CHANGE");
        String adminToken = login("admin");

        mockMvc.perform(get("/api/auth/profile").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions[0]").value("*"));
        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/system/users/{id}/roles", adminId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + roleId + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(put("/api/system/users/{id}", adminId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Changed\",\"status\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(put("/api/system/users/{id}/kickout", adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(delete("/api/system/users/{id}", adminId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?", Integer.class, adminId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT nickname FROM sys_user WHERE id = ?", String.class, adminId))
                .isEqualTo("Administrator");
    }

    @Test
    void preissuedTokenLosesProtectedAccessWhenUserIsDisabledOrDeleted() throws Exception {
        long disabledUserId = insertUser("rbac-token-disabled");
        String disabledToken = login("rbac-token-disabled");

        mockMvc.perform(get("/api/auth/profile").header("Authorization", bearer(disabledToken)))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE sys_user SET status = 0 WHERE id = ?", disabledUserId);
        mockMvc.perform(get("/api/auth/profile").header("Authorization", bearer(disabledToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        long deletedUserId = insertUser("rbac-token-deleted");
        String deletedToken = login("rbac-token-deleted");
        jdbcTemplate.update("UPDATE sys_user SET deleted = 1 WHERE id = ?", deletedUserId);
        mockMvc.perform(get("/api/auth/profile").header("Authorization", bearer(deletedToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void revokesPermissionsWhenAssignedRoleIsDisabledOrDeletedAfterLogin() throws Exception {
        long userId = insertUser("rbac-role-lifecycle");
        long roleId = insertRole("RBAC_ROLE_LIFECYCLE");
        long menuId = insertMenu("RBAC Lifecycle List", "system:user:list");
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        String token = login("rbac-role-lifecycle");

        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE sys_role SET status = 0 WHERE id = ?", roleId);
        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        jdbcTemplate.update("UPDATE sys_role SET status = 1 WHERE id = ?", roleId);
        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE sys_role SET deleted = 1 WHERE id = ?", roleId);
        mockMvc.perform(get("/api/system/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void validatesAndSoftDeletesUsers() throws Exception {
        String adminToken = login("admin");

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"short\",\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        MvcResult created = mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rbac-created\",\"password\":\"change-me-123\",\"nickname\":\"Created user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("rbac-created"))
                .andReturn();
        long userId = jsonId(created);

        mockMvc.perform(delete("/api/system/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM sys_user WHERE id = ?", Long.class, userId))
                .isEqualTo(userId);
        awaitOperations("Create user", "Delete user");
    }

    @Test
    void assignsMenusToRolesAuditsChangesAndPreservesSuperAdmin() throws Exception {
        String adminToken = login("admin");
        MvcResult role = mockMvc.perform(post("/api/system/roles")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"RBAC editable role\",\"code\":\"RBAC_EDITABLE\",\"sortOrder\":5}"))
                .andExpect(status().isOk())
                .andReturn();
        long roleId = jsonId(role);
        long menuId = insertMenu("RBAC Managed Menu", "system:menu:update");

        mockMvc.perform(put("/api/system/roles/{id}/menus", roleId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[" + menuId + "]}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ? AND menu_id = ?", Integer.class, roleId, menuId))
                .isEqualTo(1);

        mockMvc.perform(delete("/api/system/roles/1").header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        awaitOperations("Create role", "Assign role menus");
    }

    @Test
    void rejectsInactiveAssignmentsAndAuditsExpectedFailuresWithTheirHttpStatus() throws Exception {
        String adminToken = login("admin");
        long userId = insertUser("rbac-assignment-user");
        long disabledRoleId = insertRole("RBAC_DISABLED_ASSIGNMENT");
        jdbcTemplate.update("UPDATE sys_role SET status = 0 WHERE id = ?", disabledRoleId);
        long editableRoleId = insertRole("RBAC_EDITABLE_ASSIGNMENT");
        long disabledMenuId = insertMenu("RBAC Disabled Assignment Menu", "system:menu:update");
        jdbcTemplate.update("UPDATE sys_menu SET status = 0 WHERE id = ?", disabledMenuId);

        mockMvc.perform(put("/api/system/users/{id}/roles", userId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + disabledRoleId + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(put("/api/system/roles/{id}/menus", editableRoleId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[" + disabledMenuId + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?", Integer.class, userId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ?", Integer.class, editableRoleId))
                .isZero();
        awaitFailedOperationAudit("Assign user roles", 400);
        awaitFailedOperationAudit("Assign role menus", 400);
    }

    private long insertUser(String username) {
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, must_change_password) VALUES (?, ?, ?, 0)",
                username, BCrypt.hashpw("password-123", BCrypt.gensalt()), username);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private long insertRole(String code) {
        jdbcTemplate.update("INSERT INTO sys_role (name, code) VALUES (?, ?)", code, code);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE code = ?", Long.class, code);
    }

    private long insertMenu(String title, String permission) {
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, title, menu_type, permission) VALUES (0, ?, 'BUTTON', ?)",
                title, permission);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE title = ?", Long.class, title);
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

    private static long jsonId(MvcResult result) throws Exception {
        return Long.parseLong(result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"id\\\"\\s*:\\s*\\\"?(\\d+)\\\"?.*", "$1"));
    }

    private void awaitOperations(String... operations) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            List<String> recorded = jdbcTemplate.queryForList(
                    "SELECT operation FROM sys_oper_log WHERE module = 'System'", String.class);
            if (recorded.containsAll(List.of(operations))) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for audited operations " + List.of(operations));
    }

    private void awaitFailedOperationAudit(String operation, int responseCode) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            Integer count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM sys_oper_log
                            WHERE module = 'System' AND operation = ? AND response_code = ?
                              AND status = 0 AND request_params = '[redacted]'
                            """, Integer.class, operation, responseCode);
            if (count != null && count > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for failed audit of " + operation);
    }
}
