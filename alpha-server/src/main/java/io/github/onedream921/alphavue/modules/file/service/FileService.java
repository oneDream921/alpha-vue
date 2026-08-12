package io.github.onedream921.alphavue.modules.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.file.entity.SysFile;
import io.github.onedream921.alphavue.modules.file.mapper.SysFileMapper;
import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.github.onedream921.alphavue.modules.file.storage.StorageProvider;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import io.github.onedream921.alphavue.modules.system.vo.UserSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件管理服务
 */
@Service
public class FileService extends ServiceImpl<SysFileMapper, SysFile> {

    private static final Set<String> AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final FileStorageProperties properties;
    private final Map<String, StorageProvider> providers;
    private final SysUserMapper userMapper;
    private final FileAccessTokenService accessTokenService;
    private final SystemSettingService settingService;

    public FileService(FileStorageProperties properties, List<StorageProvider> storageProviders,
                       SysUserMapper userMapper,
                       FileAccessTokenService accessTokenService) {
        this(properties, storageProviders, userMapper, accessTokenService, (SystemSettingService) null);
    }

    @Autowired
    public FileService(FileStorageProperties properties, List<StorageProvider> storageProviders,
                       SysUserMapper userMapper, FileAccessTokenService accessTokenService,
                       SystemSettingService settingService) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.accessTokenService = accessTokenService;
        this.settingService = settingService;
        this.providers = storageProviders.stream().collect(Collectors.toUnmodifiableMap(
                provider -> normalizeProvider(provider.name()), provider -> provider));
    }

    /**
     * 校验并上传文件，成功后保存文件元数据
     */
    public FileView upload(MultipartFile file, long uploaderId) {
        return upload(file, uploaderId, null);
    }

    public StorageTestResult testStorage() {
        String key = ".alpha-storage-test/" + UUID.randomUUID() + ".txt";
        StorageProvider provider = providerFor(providerName());
        byte[] payload = "alpha-storage-test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            provider.store(key, new java.io.ByteArrayInputStream(payload), "text/plain");
            try (InputStream input = provider.open(key)) {
                byte[] actual = input.readAllBytes();
                if (!java.util.Arrays.equals(payload, actual)) {
                    throw new IOException("Storage test content mismatch");
                }
            }
            return new StorageTestResult(true, provider.name(), "连接、写入、读取和删除测试通过");
        } catch (Exception exception) {
            return new StorageTestResult(false, provider.name(), "存储测试失败，请检查存储配置和服务状态");
        } finally {
            try { provider.delete(key); } catch (Exception ignored) { }
        }
    }

    private FileView upload(MultipartFile file, long uploaderId, Set<String> extensionOverride) {
        String originalName = requireOriginalName(file);
        String extension = extensionOf(originalName);
        String contentType = validateUpload(file, extension, extensionOverride);
        StorageProvider provider = providerFor(providerName());
        String key = UUID.randomUUID() + "." + extension;
        try (var input = file.getInputStream()) {
            provider.store(key, input, contentType);
        } catch (IOException exception) {
            throw storageFailure(exception);
        }

        SysFile metadata = new SysFile();
        metadata.setStorageProvider(providerName());
        metadata.setObjectKey(key);
        metadata.setOriginalName(originalName);
        metadata.setContentType(contentType);
        metadata.setSizeBytes(file.getSize());
        metadata.setPublicUrl(provider.publicUrl(key));
        metadata.setUploaderId(uploaderId);
        try {
            if (!save(metadata)) {
                throw storageFailure(null);
            }
        } catch (RuntimeException exception) {
            try {
                provider.delete(key);
            } catch (IOException ignored) {
                // 保留原始元数据持久化异常，避免泄露存储实现细节。
            }
            throw exception;
        }
        return view(metadata, null);
    }

    /**
     * 校验头像格式后上传图片并保存文件元数据
     */
    public FileView uploadAvatar(MultipartFile file, long uploaderId) {
        String originalName = requireOriginalName(file);
        if (!AVATAR_EXTENSIONS.contains(extensionOf(originalName))) {
            throw invalidRequest();
        }
        return upload(file, uploaderId, AVATAR_EXTENSIONS);
    }

    /**
     * 分页查询未删除的文件元数据
     */
    public PageResponse<FileView> page(int pageNumber, int pageSize) {
        Page<SysFile> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysFile>().orderByDesc(SysFile::getId));
        Set<Long> uploaderIds = page.getRecords().stream()
                .map(SysFile::getUploaderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> uploaderNames = uploaderIds.isEmpty() ? Map.of()
                : userMapper.selectActiveSummariesByIds(uploaderIds).stream()
                        .collect(Collectors.toMap(UserSummaryVo::id, FileService::displayName));
        return new PageResponse<>(page.getRecords().stream()
                .map(file -> view(file, uploaderNames.get(file.getUploaderId())))
                .toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 返回私有文件的短期访问地址。
     */
    public String accessUrl(long id) {
        SysFile file = getById(id);
        if (file == null) {
            throw invalidRequest();
        }
        return accessUrl(file);
    }

    /**
     * 校验访问签名后打开文件内容，调用方负责关闭流。
     */
    public FileContent openForAccess(long id, long expiresAt, String signature) {
        SysFile file = getById(id);
        if (file == null || !accessTokenService.isValid(id, expiresAt, signature)) {
            throw invalidRequest();
        }
        try {
            return new FileContent(providerFor(file.getStorageProvider()).open(file.getObjectKey()), file.getOriginalName(),
                    file.getContentType());
        } catch (IOException exception) {
            throw storageFailure(exception);
        }
    }

    /**
     * 删除存储对象并软删除文件元数据
     */
    public void delete(long id) {
        SysFile metadata = getById(id);
        if (metadata == null) {
            throw invalidRequest();
        }
        try {
            providerFor(metadata.getStorageProvider()).delete(metadata.getObjectKey());
        } catch (IOException exception) {
            throw storageFailure(exception);
        }
        if (!removeById(id)) {
            int recovered = baseMapper.markDeletedIfActive(id);
            if (recovered == 0 && baseMapper.countActiveById(id) > 0) {
                throw reconciliationFailure(id, "metadata recovery could not mark the file deleted");
            }
            throw reconciliationFailure(id, "metadata recovery completed");
        }
    }

    private String validateUpload(MultipartFile file, String extension, Set<String> extensionOverride) {
        if (file == null || file.isEmpty()) {
            throw invalidRequest("上传文件不能为空");
        }
        if (file.getSize() > maxSizeBytes()) {
            throw invalidRequest("文件大小超过上传限制");
        }
        if (!(extensionOverride == null ? allowedExtensions() : extensionOverride).contains(extension)) {
            throw invalidRequest("文件扩展名不在允许范围内");
        }
        String contentType = properties.safeContentTypeForExtension(extension);
        if (contentType == null || !contentType.equalsIgnoreCase(file.getContentType())) {
            throw invalidRequest("文件类型与扩展名不匹配");
        }
        validateImageSignature(file, extension);
        return contentType;
    }

    private static void validateImageSignature(MultipartFile file, String extension) {
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            return;
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (extension) {
                case "png" -> hasPrefix(header, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
                case "jpg", "jpeg" -> hasPrefix(header, 0xff, 0xd8, 0xff);
                case "gif" -> hasPrefix(header, 'G', 'I', 'F', '8')
                        && header.length >= 6 && (header[4] == '7' || header[4] == '9') && header[5] == 'a';
                case "webp" -> hasPrefix(header, 'R', 'I', 'F', 'F')
                        && hasPrefixAt(header, 8, 'W', 'E', 'B', 'P');
                default -> false;
            };
            if (!valid) {
                throw invalidRequest("图片内容签名校验失败");
            }
        } catch (IOException exception) {
            throw invalidRequest("图片内容读取失败");
        }
    }

    private static boolean hasPrefix(byte[] value, int... prefix) {
        return hasPrefixAt(value, 0, prefix);
    }

    private static boolean hasPrefixAt(byte[] value, int offset, int... prefix) {
        if (offset < 0 || value.length < offset + prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((value[offset + index] & 0xff) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String requireOriginalName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw invalidRequest();
        }
        return file.getOriginalFilename();
    }

    private Set<String> allowedExtensions() {
        Object configured = setting("allowedExtensions");
        String source = configured instanceof String value && !value.isBlank() ? value
                : String.join(",", properties.getAllowedExtensions());
        return java.util.Arrays.stream(source.split(","))
                .filter(extension -> extension != null && !extension.isBlank())
                .map(FileService::normalizeExtension)
                .collect(Collectors.toUnmodifiableSet());
    }

    private long maxSizeBytes() {
        Object configured = setting("maxSizeMb");
        if (configured instanceof Number value && value.longValue() > 0) return value.longValue() * 1024L * 1024L;
        if (configured instanceof String value) try { return Long.parseLong(value) * 1024L * 1024L; } catch (NumberFormatException ignored) { }
        return properties.getMaxSizeBytes();
    }

    private String providerName() {
        Object configured = setting("provider");
        return configured instanceof String value && !value.isBlank() ? normalizeProvider(value) : normalizeProvider(properties.getProvider());
    }

    private Object setting(String key) {
        return settingService == null ? null : settingService.get(SettingGroup.FILE).values().get(key);
    }

    private StorageProvider providerFor(String name) {
        StorageProvider provider = providers.get(normalizeProvider(name));
        if (provider == null) {
            throw storageFailure(null);
        }
        return provider;
    }

    private static String extensionOf(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 1 || dot == originalName.length() - 1) {
            throw invalidRequest();
        }
        return normalizeExtension(originalName.substring(dot + 1));
    }

    private static String normalizeExtension(String extension) {
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    private static String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }

    private static BusinessException invalidRequest(String auditSummary) {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST, auditSummary);
    }

    private static BusinessException storageFailure(Exception cause) {
        BusinessException exception = new BusinessException(500, PublicErrorMessage.INTERNAL_SERVER_ERROR);
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    private static BusinessException reconciliationFailure(long id, String outcome) {
        return storageFailure(new IllegalStateException("Storage object was deleted; " + outcome + " for file id " + id));
    }

    private static String displayName(UserSummaryVo user) {
        return user.nickname() == null || user.nickname().isBlank() ? user.username() : user.nickname();
    }

    private FileView view(SysFile file, String uploaderName) {
        return new FileView(file.getId(), file.getStorageProvider(), file.getObjectKey(), file.getOriginalName(),
                file.getContentType(), file.getSizeBytes(), accessUrl(file), uploaderName, file.getCreatedAt());
    }

    private String accessUrl(SysFile file) {
        return properties.isPublicAccess() ? file.getPublicUrl() : accessTokenService.accessUrl(file.getId());
    }

    /**
     * 文件接口响应视图
     */
    public record FileView(Long id, String storageProvider, String objectKey, String originalName, String contentType,
                           Long sizeBytes, String publicUrl, String uploaderName, java.time.LocalDateTime createdAt) { }

    public record FileContent(InputStream input, String originalName, String contentType) { }
    public record StorageTestResult(boolean success, String provider, String message) { }
}
