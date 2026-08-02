package io.github.onedream921.alphavue.modules.maintenance.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnailJobMaintenanceExecutorTests {

    @Test
    void returnsSuccessWhenAllTasksComplete() {
        ApplicationMaintenanceService service = null;
        SnailJobMaintenanceExecutor executor = new SnailJobMaintenanceExecutor(
                new StubMaintenanceService(new MaintenanceRunReport(Instant.now(), List.of(
                        new MaintenanceTaskReport("health-summary", true, true, 1, 0, 0, "OK", "ok")))));

        assertThat(executor.execute().getStatus()).isEqualTo(1);
    }

    @Test
    void returnsFailureWhenAnyTaskFails() {
        SnailJobMaintenanceExecutor executor = new SnailJobMaintenanceExecutor(
                new StubMaintenanceService(new MaintenanceRunReport(Instant.now(), List.of(
                        MaintenanceTaskReport.failed("log-retention", new IllegalStateException("test"))))));

        assertThat(executor.execute().getStatus()).isNotEqualTo(1);
    }

    private static final class StubMaintenanceService extends ApplicationMaintenanceService {
        private final MaintenanceRunReport report;

        private StubMaintenanceService(MaintenanceRunReport report) {
            super(null, null, null, null, null, null);
            this.report = report;
        }

        @Override
        public MaintenanceRunReport runAll() {
            return report;
        }
    }
}
