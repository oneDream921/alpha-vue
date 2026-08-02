package io.github.onedream921.alphavue.modules.maintenance.service;

/**
 * Bounded summary for one maintenance task run.
 */
public record MaintenanceTaskReport(String task, boolean enabled, boolean dryRun, int scanned, int affected,
                                    int skipped, String status, String message) {

    public static MaintenanceTaskReport skipped(String task, String message) {
        return new MaintenanceTaskReport(task, false, true, 0, 0, 0, "SKIPPED", message);
    }

    public static MaintenanceTaskReport failed(String task, RuntimeException exception) {
        return new MaintenanceTaskReport(task, true, true, 0, 0, 0, "FAILED",
                exception.getClass().getSimpleName());
    }
}
