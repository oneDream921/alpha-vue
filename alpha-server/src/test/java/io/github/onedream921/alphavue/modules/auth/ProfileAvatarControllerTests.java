package io.github.onedream921.alphavue.modules.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 个人头像接口测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileAvatarControllerTests {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            0x00, 0x00, 0x00, 0x00
    };

    @TempDir
    static Path uploadRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void fileProperties(DynamicPropertyRegistry registry) {
        registry.add("alpha.file.provider", () -> "local");
        registry.add("alpha.file.local-root", uploadRoot::toString);
        registry.add("alpha.file.local-public-url", () -> "/uploads");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_file WHERE uploader_id = (SELECT id FROM sys_user WHERE username = ?)",
                "profile-avatar-user");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", "profile-avatar-user");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", "profile-phone-user");
    }

    @Test
    void allowsAnOrdinaryUserToUploadTheirOwnAvatarWithoutFileManagementPermission() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO sys_user (username, password, nickname, status, must_change_password, deleted)
                VALUES (?, ?, ?, 1, 0, 0)
                """, "profile-avatar-user", BCrypt.hashpw("avatar123", BCrypt.gensalt()), "普通用户");

        MockMultipartFile avatar = new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, PNG);
        mockMvc.perform(multipart("/api/auth/avatar").file(avatar).header("Authorization", bearer(login("profile-avatar-user", "avatar123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.avatar").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    void rejectsAnInvalidPhoneNumberWhenUpdatingProfile() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO sys_user (username, password, nickname, status, must_change_password, deleted)
                VALUES (?, ?, ?, 1, 0, 0)
                """, "profile-phone-user", BCrypt.hashpw("phone123", BCrypt.gensalt()), "手机号用户");

        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", bearer(login("profile-phone-user", "phone123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"手机号用户\",\"phone\":\"112\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
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
}
