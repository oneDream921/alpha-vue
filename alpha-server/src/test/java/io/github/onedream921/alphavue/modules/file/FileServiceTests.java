package io.github.onedream921.alphavue.modules.file;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the externally observable file-upload contract against the real local provider. */
@SpringBootTest(properties = {
        "alpha.file.provider=local",
        "alpha.file.allowed-extensions=txt,png",
        "alpha.file.max-size-bytes=4",
        "alpha.file.local-root=target/file-service-tests",
        "alpha.file.local-public-url=/uploads"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileServiceTests {

    private static final Path STORAGE_ROOT = Path.of("target/file-service-tests").toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileService fileService;

    @Autowired
    private LocalStorageProvider localStorageProvider;

    @Autowired
    private MinioStorageProvider minioStorageProvider;

    @AfterEach
    void cleanUp() throws IOException {
        jdbcTemplate.update("DELETE FROM sys_file");
        if (Files.exists(STORAGE_ROOT)) {
            try (var files = Files.walk(STORAGE_ROOT)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                });
            }
        }
    }

    @Test
    void rejectsAnExtensionOutsideTheConfiguredAllowList() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "payload.exe", "application/octet-stream", new byte[] {1});

        mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_file", Integer.class)).isZero();
    }

    @Test
    void rejectsAFileLargerThanTheConfiguredMaximum() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "payload.txt", MediaType.TEXT_PLAIN_VALUE,
                new byte[] {1, 2, 3, 4, 5});

        mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_file", Integer.class)).isZero();
    }

    @Test
    void storesAnAllowedUploadLocallyAndPersistsItsUuidMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.txt", MediaType.TEXT_PLAIN_VALUE,
                new byte[] {1, 2, 3, 4});

        MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originalName").value("receipt.txt"))
                .andExpect(jsonPath("$.data.storageProvider").value("local"))
                .andReturn();

        long id = jsonId(result);
        String key = jdbcTemplate.queryForObject("SELECT object_key FROM sys_file WHERE id = ?", String.class, id);
        assertThat(key).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.txt");
        assertThat(Files.readAllBytes(STORAGE_ROOT.resolve(key))).containsExactly(1, 2, 3, 4);
        assertThat(jdbcTemplate.queryForObject("SELECT public_url FROM sys_file WHERE id = ?", String.class, id))
                .isEqualTo("/uploads/" + key);
    }

    @Test
    void keepsMetadataActiveWhenDeletingTheStoredObjectFails() throws Exception {
        Files.createDirectories(STORAGE_ROOT.resolve("blocked"));
        Files.writeString(STORAGE_ROOT.resolve("blocked/child.txt"), "not empty");
        jdbcTemplate.update("""
                INSERT INTO sys_file (storage_provider, object_key, original_name, size_bytes)
                VALUES ('local', 'blocked', 'blocked.txt', 1)
                """);
        long id = jdbcTemplate.queryForObject("SELECT id FROM sys_file WHERE object_key = 'blocked'", Long.class);

        assertThatThrownBy(() -> fileService.delete(id)).isInstanceOf(BusinessException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM sys_file WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void preventsLocalStorageKeysFromTraversingOutsideTheConfiguredRoot() throws Exception {
        Path escaped = STORAGE_ROOT.getParent().resolve("escaped.txt");
        Files.deleteIfExists(escaped);

        assertThatThrownBy(() -> localStorageProvider.store("../escaped.txt", new ByteArrayInputStream(new byte[] {1}),
                MediaType.TEXT_PLAIN_VALUE)).isInstanceOf(IOException.class);

        assertThat(Files.exists(escaped)).isFalse();
    }

    @Test
    void constructsMinioProviderWithoutConnectingToMinio() {
        assertThat(minioStorageProvider.publicUrl("example.txt"))
                .isEqualTo("http://localhost:9000/alpha-vue/example.txt");
    }

    private String loginAsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString().replaceFirst("(?s).*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static long jsonId(MvcResult result) throws Exception {
        return Long.parseLong(result.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"id\\\"\\s*:\\s*(\\d+).*", "$1"));
    }
}
