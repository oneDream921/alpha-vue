package io.github.onedream921.alphavue.modules.maintenance.service;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SnailJob entry point for one bounded application maintenance cycle.
 */
@Component
@ConditionalOnProperty(prefix = "snail-job", name = "enabled", havingValue = "true")
public class SnailJobMaintenanceExecutor {

    private static final Logger log = LoggerFactory.getLogger(SnailJobMaintenanceExecutor.class);

    private final ApplicationMaintenanceService service;

    public SnailJobMaintenanceExecutor(ApplicationMaintenanceService service) {
        this.service = service;
    }

    @JobExecutor(name = "alphaMaintenanceJob")
    public ExecuteResult execute() {
        MaintenanceRunReport report = service.runAll();
        report.tasks().forEach(task -> log.info(
                "SnailJob maintenance task {} status={} dryRun={} scanned={} affected={} skipped={} message={}",
                task.task(), task.status(), task.dryRun(), task.scanned(), task.affected(), task.skipped(),
                task.message()));

        boolean failed = report.tasks().stream().anyMatch(task -> "FAILED".equals(task.status()));
        if (failed) {
            return ExecuteResult.failure("One or more maintenance tasks failed");
        }
        return ExecuteResult.success("Maintenance cycle completed");
    }
}
