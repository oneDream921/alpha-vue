package io.github.onedream921.alphavue.modules.file;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Autowired
    private FileStorageProperties fileStorageProperties;

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
    void rejectsAPlainTextExtensionWithAnActiveClientControlledMimeType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "report.txt", MediaType.TEXT_HTML_VALUE,
                "<b>".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_file", Integer.class)).isZero();
    }

    @Test
    void rejectsAnImageWhoseBytesDoNotMatchItsDeclaredSafeImageType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE,
                "nope".getBytes());

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
    void preventsLocalStorageKeysFromEscapingThroughASymlink() throws Exception {
        Path externalRoot = Files.createTempDirectory("file-storage-external-");
        Path link = STORAGE_ROOT.resolve("linked");
        Files.createDirectories(STORAGE_ROOT);
        Files.createSymbolicLink(link, externalRoot);
        Path escaped = externalRoot.resolve("escaped.txt");
        try {
            assertThatThrownBy(() -> localStorageProvider.store("linked/escaped.txt",
                    new ByteArrayInputStream(new byte[] {1}), MediaType.TEXT_PLAIN_VALUE))
                    .isInstanceOf(IOException.class);

            assertThat(Files.exists(escaped)).isFalse();
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(escaped);
            Files.deleteIfExists(externalRoot);
        }
    }

    @Test
    void rejectsMinioSelectionWithoutExplicitApplicationCredentials() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setProvider(MinioStorageProvider.NAME);

        assertThatThrownBy(properties::validateForActiveProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MINIO_ACCESS_KEY")
                .hasMessageContaining("MINIO_SECRET_KEY");
    }

    @Test
    void reconcilesMetadataWhenTheNormalSoftDeleteReportsNoUpdatedRow() throws Exception {
        String key = "reconcile.txt";
        Files.createDirectories(STORAGE_ROOT);
        Files.writeString(STORAGE_ROOT.resolve(key), "file");
        jdbcTemplate.update("""
                INSERT INTO sys_file (storage_provider, object_key, original_name, size_bytes)
                VALUES ('local', 'reconcile.txt', 'reconcile.txt', 4)
                """);
        long id = jdbcTemplate.queryForObject("SELECT id FROM sys_file WHERE object_key = ?", Long.class, key);
        FileService service = new FailedSoftDeleteFileService(fileStorageProperties, localStorageProvider, minioStorageProvider);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", fileService.getBaseMapper());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseMessage("Storage object was deleted; metadata recovery completed for file id " + id);

        assertThat(Files.exists(STORAGE_ROOT.resolve(key))).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM sys_file WHERE id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void constructsMinioProviderWithoutConnectingToMinio() {
        assertThat(minioStorageProvider.publicUrl("example.txt"))
                .isEqualTo("http://localhost:9000/alpha-vue/example.txt");
    }

    @Test
    void minioUsesTheServerDerivedContentTypeInsteadOfTheProvidedValue() throws Exception {
        MinioClient client = mock(MinioClient.class);
        org.springframework.test.util.ReflectionTestUtils.setField(minioStorageProvider, "client", client);

        minioStorageProvider.store("safe.txt", new ByteArrayInputStream(new byte[] {1}), MediaType.TEXT_HTML_VALUE);

        org.mockito.ArgumentCaptor<PutObjectArgs> arguments = org.mockito.ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(arguments.capture());
        assertThat(arguments.getValue().contentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    }

    private static final class FailedSoftDeleteFileService extends FileService {
        private FailedSoftDeleteFileService(FileStorageProperties properties, LocalStorageProvider localStorageProvider,
                                            MinioStorageProvider minioStorageProvider) {
            super(properties, localStorageProvider, minioStorageProvider);
        }

        @Override
        public boolean removeById(Serializable id) {
            return false;
        }
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
