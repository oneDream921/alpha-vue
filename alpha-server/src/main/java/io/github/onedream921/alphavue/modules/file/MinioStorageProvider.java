package io.github.onedream921.alphavue.modules.file;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/** MinIO-backed implementation that creates no network connection until an object operation is requested. */
@Component
public class MinioStorageProvider implements StorageProvider {

    public static final String NAME = "minio";

    private final FileStorageProperties.Minio properties;
    private final MinioClient client;

    public MinioStorageProvider(FileStorageProperties fileProperties) {
        this.properties = fileProperties.getMinio();
        this.client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public void store(String key, InputStream input, String contentType) throws IOException {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(input, -1, 10 * 1024 * 1024)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw new IOException("Unable to store object in MinIO", exception);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(properties.getBucket()).object(key).build());
        } catch (Exception exception) {
            throw new IOException("Unable to delete object from MinIO", exception);
        }
    }

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
}
