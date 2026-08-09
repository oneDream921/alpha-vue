package io.github.onedream921.alphavue.modules.file.storage;

import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * MinIO 文件存储实现
 */
@Component
public class MinioStorageProvider implements StorageProvider {

    public static final String NAME = "minio";

    private final FileStorageProperties fileProperties;
    private final SystemSettingService settings;
    private volatile MinioClient client;
    private volatile String clientSignature;

    /**
     * 根据文件存储配置初始化 MinIO 提供者
     */
    public MinioStorageProvider(FileStorageProperties fileProperties, SystemSettingService settings) {
        this.fileProperties = fileProperties;
        this.settings = settings;
    }

    @Override
    public String name() {
        return NAME;
    }

    /**
     * 将对象内容写入 MinIO
     */
    @Override
    public void store(String key, InputStream input, String contentType) throws IOException {
        try {
            String serverContentType = fileProperties.safeContentTypeForKey(key);
            if (serverContentType == null) {
                throw new IOException("Storage key has no allowed server-side content type");
            }
            client().putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(key)
                    .stream(input, -1, 10 * 1024 * 1024)
                    .contentType(serverContentType)
                    .build());
        } catch (Exception exception) {
            throw new IOException("Unable to store object in MinIO", exception);
        }
    }

    /**
     * 删除 MinIO 中的对象
     */
    @Override
    public void delete(String key) throws IOException {
        try {
            client().removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(key).build());
        } catch (Exception exception) {
            throw new IOException("Unable to delete object from MinIO", exception);
        }
    }

    @Override
    public InputStream open(String key) throws IOException {
        try {
            return client().getObject(GetObjectArgs.builder().bucket(bucket()).object(key).build());
        } catch (Exception exception) {
            throw new IOException("Unable to read object from MinIO", exception);
        }
    }

    /**
     * 返回 MinIO 对象的公开访问地址
     */
    @Override
    public String publicUrl(String key) {
        Map<String, Object> value = settings.runtimeValues(SettingGroup.FILE);
        String base = text(value, "accessDomain", fileProperties.getMinio().getPublicUrl());
        if (base == null || base.isBlank()) {
            base = text(value, "endpoint", fileProperties.getMinio().getEndpoint());
        }
        return trimTrailingSlash(base) + "/" + text(value, "bucket", fileProperties.getMinio().getBucket()) + "/" + key;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private MinioClient client() {
        MinioClient current = client;
        if (current != null && clientSignature == null) return current;
        Map<String, Object> value = settings.runtimeValues(SettingGroup.FILE);
        String endpoint = text(value, "endpoint", fileProperties.getMinio().getEndpoint());
        String accessKey = required(value, "accessKey", fileProperties.getMinio().getAccessKey());
        String secretKey = required(value, "secretKey", fileProperties.getMinio().getSecretKey());
        String signature = endpoint + "\n" + accessKey + "\n" + secretKey;
        if (current != null && (clientSignature == null || clientSignature.equals(signature))) return current;
        synchronized (this) {
            if (client == null || !signature.equals(clientSignature)) {
                client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
                clientSignature = signature;
            }
            return client;
        }
    }

    private String bucket() { return text(settings.runtimeValues(SettingGroup.FILE), "bucket", fileProperties.getMinio().getBucket()); }
    private static String text(Map<String, Object> value, String key, String fallback) {
        Object configured = value.get(key);
        return configured instanceof String text && !text.isBlank() ? text.trim() : fallback;
    }
    private static String required(Map<String, Object> value, String key, String fallback) {
        String result = text(value, key, fallback);
        if (result == null || result.isBlank()) throw new IllegalStateException("Missing MinIO setting: " + key);
        return result;
    }
}
