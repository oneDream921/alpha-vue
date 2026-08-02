package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Coordinates one bounded maintenance cycle.
 */
@Service
public class ApplicationMaintenanceService {

    private final MaintenanceProperties properties;
    private final LogRetentionMaintenanceService logRetention;
    private final TempFileMaintenanceService tempFiles;
    private final StorageConsistencyMaintenanceService storageConsistency;
    private final SessionIndexMaintenanceService sessionIndexes;
    private final HealthSummaryMaintenanceService healthSummary;

    public ApplicationMaintenanceService(MaintenanceProperties properties, LogRetentionMaintenanceService logRetention,
            TempFileMaintenanceService tempFiles, StorageConsistencyMaintenanceService storageConsistency,
            SessionIndexMaintenanceService sessionIndexes, HealthSummaryMaintenanceService healthSummary) {
        this.properties = properties;
        this.logRetention = logRetention;
        this.tempFiles = tempFiles;
        this.storageConsistency = storageConsistency;
        this.sessionIndexes = sessionIndexes;
        this.healthSummary = healthSummary;
    }

    public MaintenanceRunReport runAll() {
        if (!properties.isEnabled()) {
            return new MaintenanceRunReport(Instant.now(), List.of(
                    MaintenanceTaskReport.skipped("maintenance", "disabled")));
        }
        List<MaintenanceTaskReport> reports = new ArrayList<>();
        reports.add(run("log-retention", logRetention::run));
        reports.add(run("temp-file-cleanup", tempFiles::run));
        reports.add(run("storage-consistency", storageConsistency::run));
        reports.add(run("session-index-repair", sessionIndexes::run));
        reports.add(run("health-summary", healthSummary::run));
        return new MaintenanceRunReport(Instant.now(), List.copyOf(reports));
    }

    private static MaintenanceTaskReport run(String task, Supplier<MaintenanceTaskReport> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            return MaintenanceTaskReport.failed(task, exception);
        }
    }
}
