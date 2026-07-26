package io.github.onedream921.alphavue.modules.file.storage;

import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * MinIO 文件存储实现
 */
@Component
public class MinioStorageProvider implements StorageProvider {

    public static final String NAME = "minio";

    private final FileStorageProperties.Minio properties;
    private final FileStorageProperties fileProperties;
    private volatile MinioClient client;

    /**
     * 根据文件存储配置初始化 MinIO 提供者
     */
    public MinioStorageProvider(FileStorageProperties fileProperties) {
        this.fileProperties = fileProperties;
        this.properties = fileProperties.getMinio();
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
                    .bucket(properties.getBucket())
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
            client().removeObject(RemoveObjectArgs.builder().bucket(properties.getBucket()).object(key).build());
        } catch (Exception exception) {
            throw new IOException("Unable to delete object from MinIO", exception);
        }
    }

    /**
     * 返回 MinIO 对象的公开访问地址
     */
    @Override
    public String publicUrl(String key) {
        String base = properties.getPublicUrl();
        if (base == null || base.isBlank()) {
            base = properties.getEndpoint();
        }
        return trimTrailingSlash(base) + "/" + properties.getBucket() + "/" + key;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private MinioClient client() {
        MinioClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                if (properties.getAccessKey() == null || properties.getAccessKey().isBlank()
                        || properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
                    throw new IllegalStateException("MINIO_ACCESS_KEY and MINIO_SECRET_KEY are required for MinIO storage");
                }
                client = MinioClient.builder()
                        .endpoint(properties.getEndpoint())
                        .credentials(properties.getAccessKey(), properties.getSecretKey())
                        .build();
            }
            return client;
        }
    }
}
