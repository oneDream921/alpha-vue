package io.github.onedream921.alphavue.modules.file;

import lombok.Getter;
import lombok.Setter;
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

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "alpha-vue";
        private String publicUrl = "";
    }
}
