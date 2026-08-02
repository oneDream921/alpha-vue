package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the application maintenance cycle on a fixed delay.
 */
@Component
@ConditionalOnProperty(prefix = "alpha.maintenance", name = "spring-scheduler-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "snail-job", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ApplicationMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApplicationMaintenanceScheduler.class);

    private final ApplicationMaintenanceService service;
    private final AuditLogService auditLogService;

    public ApplicationMaintenanceScheduler(ApplicationMaintenanceService service, AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedDelayString = "${alpha.maintenance.fixed-delay-ms:3600000}",
            initialDelayString = "${alpha.maintenance.initial-delay-ms:300000}")
    public void runScheduledMaintenance() {
        long startedAt = System.currentTimeMillis();
        MaintenanceRunReport report = service.runAll();
        for (MaintenanceTaskReport task : report.tasks()) {
            log.info("Maintenance task {} status={} dryRun={} scanned={} affected={} skipped={} message={}",
                    task.task(), task.status(), task.dryRun(), task.scanned(), task.affected(), task.skipped(),
                    task.message());
        }
        if (requiresAudit(report)) {
            auditLogService.recordOperation(null, "system", "Maintenance", "Run scheduled maintenance",
                    hasDeletion(report) ? BusinessType.DELETE : BusinessType.OTHER, "SCHEDULED",
                    "maintenance://scheduled", hasFailure(report) ? 500 : 200, !hasFailure(report),
                    null, System.currentTimeMillis() - startedAt, null, null, null, null, null,
                    null, null, null, summary(report));
        }
    }

    private static boolean requiresAudit(MaintenanceRunReport report) {
        return report.tasks().stream().anyMatch(task -> task.affected() > 0 || "FAILED".equals(task.status()));
    }

    private static boolean hasDeletion(MaintenanceRunReport report) {
        return report.tasks().stream().anyMatch(task -> task.affected() > 0 && !task.dryRun());
    }

    private static boolean hasFailure(MaintenanceRunReport report) {
        return report.tasks().stream().anyMatch(task -> "FAILED".equals(task.status()));
    }

    private static String summary(MaintenanceRunReport report) {
        int scanned = report.tasks().stream().mapToInt(MaintenanceTaskReport::scanned).sum();
        int affected = report.tasks().stream().mapToInt(MaintenanceTaskReport::affected).sum();
        int failed = (int) report.tasks().stream().filter(task -> "FAILED".equals(task.status())).count();
        String tasks = report.tasks().stream()
                .map(task -> task.task() + ':' + task.status() + ':' + task.affected())
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"captured\":true,\"scanned\":" + scanned + ",\"affected\":" + affected
                + ",\"failed\":" + failed + ",\"tasks\":\"" + tasks + "\"}";
    }
}
