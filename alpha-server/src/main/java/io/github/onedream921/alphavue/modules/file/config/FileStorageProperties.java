package io.github.onedream921.alphavue.modules.file.config;

import io.github.onedream921.alphavue.modules.file.storage.LocalStorageProvider;
import io.github.onedream921.alphavue.modules.file.storage.MinioStorageProvider;
import lombok.Getter;
import lombok.Setter;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 文件存储配置属性
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.file")
public class FileStorageProperties {

    private String provider = LocalStorageProvider.NAME;
    private Set<String> allowedExtensions = new LinkedHashSet<>(Set.of(
            "txt", "pdf", "png", "jpg", "jpeg", "gif", "webp", "doc", "docx", "xls", "xlsx"));
    private long maxSizeBytes = 10 * 1024 * 1024;
    private String localRoot = "uploads";
    private String localPublicUrl = "/uploads";
    private Minio minio = new Minio();

    /**
     * 校验当前启用的存储提供者配置是否完整
     */
    @PostConstruct
    public void validateForActiveProvider() {
        if (LocalStorageProvider.NAME.equalsIgnoreCase(provider)) {
            localPublicPathPattern();
        }
        if (MinioStorageProvider.NAME.equalsIgnoreCase(provider)
                && (isBlank(minio.getAccessKey()) || isBlank(minio.getSecretKey()))) {
            throw new IllegalStateException("MINIO_ACCESS_KEY and MINIO_SECRET_KEY must be configured when "
                    + "FILE_STORAGE_PROVIDER=minio");
        }
    }

    /**
     * 返回本地公开资源路径匹配规则
     */
    public String localPublicPathPattern() {
        String path = localPublicUrl == null || localPublicUrl.isBlank()
                ? "/uploads"
                : localPublicUrl.trim();
        if (!path.startsWith("/") || path.contains("://") || path.contains("..")) {
            throw new IllegalStateException("FILE_LOCAL_PUBLIC_URL must be an application-relative path");
        }
        String base = path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1)
                : path;
        return base + "/**";
    }

    /**
     * 返回本地文件资源目录位置
     */
    public String localResourceLocation() {
        String location = Path.of(localRoot).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    /**
     * 根据扩展名获取允许的服务端 Content-Type
     */
    public String safeContentTypeForExtension(String extension) {
        return switch (extension == null ? "" : extension.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> null;
        };
    }

    /**
     * 根据对象键获取允许的服务端 Content-Type
     */
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

    /**
     * MinIO 存储连接和公开访问配置
     */
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
