package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;

/**
 * Emits a lightweight runtime health summary without exposing credentials.
 */
@Service
public class HealthSummaryMaintenanceService {

    private final MaintenanceProperties properties;

    public HealthSummaryMaintenanceService(MaintenanceProperties properties) {
        this.properties = properties;
    }

    public MaintenanceTaskReport run() {
        if (!properties.getHealthSummary().isEnabled()) {
            return MaintenanceTaskReport.skipped("health-summary", "disabled");
        }
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        String message = "uptimeMs=" + ManagementFactory.getRuntimeMXBean().getUptime()
                + ",usedMemoryBytes=" + usedMemory
                + ",maxMemoryBytes=" + runtime.maxMemory();
        return new MaintenanceTaskReport("health-summary", true, true, 1, 0, 0, "OK", message);
    }
}
