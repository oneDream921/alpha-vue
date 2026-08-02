package io.github.onedream921.alphavue.modules.maintenance.service;

import java.time.Instant;
import java.util.List;

/**
 * Summary of one scheduled maintenance cycle.
 */
public record MaintenanceRunReport(Instant startedAt, List<MaintenanceTaskReport> tasks) {
}
