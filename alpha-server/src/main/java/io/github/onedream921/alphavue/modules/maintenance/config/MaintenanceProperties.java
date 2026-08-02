package io.github.onedream921.alphavue.modules.maintenance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime maintenance task configuration.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.maintenance")
public class MaintenanceProperties {

    private boolean enabled = true;
    private long initialDelayMs = 300_000L;
    private long fixedDelayMs = 3_600_000L;
    private LogCleanup logs = new LogCleanup();
    private TempFileCleanup tempFiles = new TempFileCleanup();
    private StorageConsistency storageConsistency = new StorageConsistency();
    private SessionIndex sessions = new SessionIndex();
    private HealthSummary healthSummary = new HealthSummary();

    public int safeBatchSize(int configured) {
        return Math.max(1, Math.min(configured, 1_000));
    }

    @Getter
    @Setter
    public static class LogCleanup {
        private boolean enabled = true;
        private boolean dryRun = true;
        private int retentionDays = 90;
        private int batchSize = 1_000;
    }

    @Getter
    @Setter
    public static class TempFileCleanup {
        private boolean enabled = true;
        private boolean dryRun = true;
        private long retentionMs = 86_400_000L;
        private int batchSize = 1_000;
    }

    @Getter
    @Setter
    public static class StorageConsistency {
        private boolean enabled = true;
        private int batchSize = 200;
    }

    @Getter
    @Setter
    public static class SessionIndex {
        private boolean enabled = true;
        private boolean dryRun = true;
        private int batchSize = 1_000;
    }

    @Getter
    @Setter
    public static class HealthSummary {
        private boolean enabled = true;
    }
}
