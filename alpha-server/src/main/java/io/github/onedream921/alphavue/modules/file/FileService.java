package io.github.onedream921.alphavue.modules.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.file.entity.SysFile;
import io.github.onedream921.alphavue.modules.file.mapper.SysFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Validates uploads, delegates object operations, and persists only file metadata. */
@Service
public class FileService extends ServiceImpl<SysFileMapper, SysFile> {

    private final FileStorageProperties properties;
    private final Map<String, StorageProvider> providers;

    public FileService(FileStorageProperties properties, LocalStorageProvider localStorageProvider,
                       MinioStorageProvider minioStorageProvider) {
        this.properties = properties;
        this.providers = Map.of(
                LocalStorageProvider.NAME, localStorageProvider,
                MinioStorageProvider.NAME, minioStorageProvider);
    }

    public FileView upload(MultipartFile file, long uploaderId) {
        String originalName = requireOriginalName(file);
        String extension = extensionOf(originalName);
        validateUpload(file, extension);
        StorageProvider provider = providerFor(properties.getProvider());
        String key = UUID.randomUUID() + "." + extension;
        try (var input = file.getInputStream()) {
            provider.store(key, input, file.getContentType());
        } catch (IOException exception) {
            throw storageFailure(exception);
        }

        SysFile metadata = new SysFile();
        metadata.setStorageProvider(normalizeProvider(properties.getProvider()));
        metadata.setObjectKey(key);
        metadata.setOriginalName(originalName);
        metadata.setContentType(file.getContentType());
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
                // Preserve the original metadata persistence failure without leaking storage internals.
            }
            throw exception;
        }
        return view(metadata);
    }

    public PageResponse<FileView> page(int pageNumber, int pageSize) {
        Page<SysFile> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysFile>().orderByDesc(SysFile::getId));
        return new PageResponse<>(page.getRecords().stream().map(FileService::view).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    @Transactional
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
        removeById(id);
    }

    private void validateUpload(MultipartFile file, String extension) {
        if (file == null || file.isEmpty() || file.getSize() > properties.getMaxSizeBytes()
                || !allowedExtensions().contains(extension)) {
            throw invalidRequest();
        }
    }

    private String requireOriginalName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw invalidRequest();
        }
        return file.getOriginalFilename();
    }

    private Set<String> allowedExtensions() {
        return properties.getAllowedExtensions().stream()
                .filter(extension -> extension != null && !extension.isBlank())
                .map(FileService::normalizeExtension)
                .collect(Collectors.toUnmodifiableSet());
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

    private static BusinessException storageFailure(Exception cause) {
        BusinessException exception = new BusinessException(500, PublicErrorMessage.INTERNAL_SERVER_ERROR);
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    private static FileView view(SysFile file) {
        return new FileView(file.getId(), file.getStorageProvider(), file.getObjectKey(), file.getOriginalName(),
                file.getContentType(), file.getSizeBytes(), file.getPublicUrl(), file.getUploaderId(), file.getCreatedAt());
    }

    public record FileView(Long id, String storageProvider, String objectKey, String originalName, String contentType,
                           Long sizeBytes, String publicUrl, Long uploaderId, java.time.LocalDateTime createdAt) { }
}
