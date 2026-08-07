package io.github.onedream921.alphavue.modules.file;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.github.onedream921.alphavue.modules.file.service.FileService;
import io.github.onedream921.alphavue.modules.file.service.FileAccessTokenService;
import io.github.onedream921.alphavue.modules.file.storage.LocalStorageProvider;
import io.github.onedream921.alphavue.modules.file.storage.MinioStorageProvider;
import io.github.onedream921.alphavue.modules.file.storage.StorageProvider;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.service.ConfigService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 使用真实本地存储实现验证外部可观察的文件上传契约。 */
@SpringBootTest(properties = {
        "alpha.file.provider=local",
        "alpha.file.allowed-extensions=txt,png,webp",
        "alpha.file.max-size-bytes=16",
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

    @Autowired
    private FileAccessTokenService fileAccessTokenService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ConfigService configService;

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

    @org.junit.jupiter.api.BeforeEach
    void configureRuntimeFileRules() {
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key LIKE 'file.%'");
        jdbcTemplate.update("INSERT INTO sys_config (config_name, config_key, config_value, config_group, data_type, enabled, deleted) VALUES (?, ?, ?, ?, ?, ?, 0)",
                "测试上传大小", "file.upload.max-size-mb", "1", "文件", "INTEGER", true);
        jdbcTemplate.update("INSERT INTO sys_config (config_name, config_key, config_value, config_group, data_type, enabled, deleted) VALUES (?, ?, ?, ?, ?, ?, 0)",
                "测试扩展名", "file.upload.allowed-extensions", "txt,png,webp", "文件", "STRING", true);
        jdbcTemplate.update("INSERT INTO sys_config (config_name, config_key, config_value, config_group, data_type, enabled, deleted) VALUES (?, ?, ?, ?, ?, ?, 0)",
                "测试访问期限", "file.private-access-ttl-minutes", "1", "文件", "INTEGER", true);
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
                new byte[1_048_577]);

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
    void storesAWebpImageWhenItsMimeTypeAndSignatureMatch() throws Exception {
        byte[] webp = new byte[] {'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P'};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.webp", "image/webp", webp);

        mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalName").value("avatar.webp"))
                .andExpect(jsonPath("$.data.contentType").value("image/webp"));

        assertThat(jdbcTemplate.queryForObject("SELECT object_key FROM sys_file", String.class))
                .endsWith(".webp");
    }

    @Test
    void avatarImageRemainsAllowedWhenOrdinaryFileAllowListExcludesPng() throws Exception {
        jdbcTemplate.update("UPDATE sys_config SET config_value = 'jpg' WHERE config_key = 'file.upload.allowed-extensions'");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE,
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        FileService.FileView uploaded = fileService.uploadAvatar(file, 1L);

        assertThat(uploaded.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_file WHERE object_key = ?", Integer.class,
                uploaded.objectKey())).isEqualTo(1);
    }

    @Test
    void rejectsAWebpImageWithoutTheRiffWebpSignature() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.webp", "image/webp",
                "not-a-webp".getBytes());

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

        mockMvc.perform(get("/api/files").header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].uploaderName").value("Administrator"));
    }

    @Test
    void servesPrivateFilesThroughShortLivedSignedUrls() throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "private.txt", MediaType.TEXT_PLAIN_VALUE,
                                "private".getBytes()))
                        .header("Authorization", bearer(loginAsAdmin())))
                .andExpect(status().isOk())
                .andReturn();
        String accessUrl = upload.getResponse().getContentAsString()
                .replaceFirst("(?s).*\\\"publicUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1")
                .replace("\\u0026", "&");

        mockMvc.perform(get(accessUrl))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("private"));

        mockMvc.perform(get("/api/files/1/content").param("expires", "1").param("signature", "invalid"))
                .andExpect(status().isBadRequest());
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
    void rejectsUnknownStorageProvider() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setProvider("s3");

        assertThatThrownBy(properties::validateForActiveProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FILE_STORAGE_PROVIDER must be local or minio");
    }

    @Test
    void validatesMinioEndpointBucketAndPublicUrlWhenEnabled() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setProvider(MinioStorageProvider.NAME);
        properties.getMinio().setAccessKey("app");
        properties.getMinio().setSecretKey("secret");
        properties.getMinio().setEndpoint("http://minio:9000");
        properties.getMinio().setBucket("Alpha_Vue");

        assertThatThrownBy(properties::validateForActiveProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MINIO_BUCKET");

        properties.getMinio().setBucket("alpha-vue");
        properties.getMinio().setPublicUrl("http://user:password@cdn.example.com");
        assertThatThrownBy(properties::validateForActiveProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MINIO_PUBLIC_URL");
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
        FileService service = new FailedSoftDeleteFileService(fileStorageProperties,
                List.of(localStorageProvider, minioStorageProvider), userMapper, fileAccessTokenService, configService);
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
        private FailedSoftDeleteFileService(FileStorageProperties properties, List<StorageProvider> storageProviders,
                                            SysUserMapper userMapper,
                                            FileAccessTokenService fileAccessTokenService, ConfigService configService) {
            super(properties, storageProviders, userMapper, fileAccessTokenService, configService);
        }

        @Override
        public boolean removeById(Serializable id) {
            return false;
        }
    }

    private String loginAsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"clientId\":\"pc-admin\"}"))
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
}
