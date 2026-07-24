package io.github.onedream921.alphavue.modules.file;

import lombok.Getter;
import lombok.Setter;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/** Configuration for selecting a storage implementation and enforcing upload limits. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.file")
public class FileStorageProperties {

    private String provider = LocalStorageProvider.NAME;
    private Set<String> allowedExtensions = new LinkedHashSet<>(Set.of(
            "txt", "pdf", "png", "jpg", "jpeg", "gif", "doc", "docx", "xls", "xlsx"));
    private long maxSizeBytes = 10 * 1024 * 1024;
    private String localRoot = "uploads";
    private String localPublicUrl = "/uploads";
    private Minio minio = new Minio();

    @PostConstruct
    public void validateForActiveProvider() {
        if (MinioStorageProvider.NAME.equalsIgnoreCase(provider)
                && (isBlank(minio.getAccessKey()) || isBlank(minio.getSecretKey()))) {
            throw new IllegalStateException("MINIO_ACCESS_KEY and MINIO_SECRET_KEY must be configured when "
                    + "FILE_STORAGE_PROVIDER=minio");
        }
    }

    public String safeContentTypeForExtension(String extension) {
        return switch (extension == null ? "" : extension.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> null;
        };
    }

    public String safeContentTypeForKey(String key) {
        if (key == null) {
            return null;
        }
        int dot = key.lastIndexOf('.');
        if (dot < 1 || dot == key.length() - 1) {
            return null;
        }
        String extension = key.substring(dot + 1);
        if (allowedExtensions.stream().noneMatch(value -> extension.equalsIgnoreCase(value))) {
            return null;
        }
        return safeContentTypeForExtension(extension);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey;
        private String secretKey;
        private String bucket = "alpha-vue";
        private String publicUrl = "";
    }
}
