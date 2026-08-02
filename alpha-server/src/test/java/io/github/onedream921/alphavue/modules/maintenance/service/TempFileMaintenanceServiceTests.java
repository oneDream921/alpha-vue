package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TempFileMaintenanceServiceTests {

    @TempDir
    private Path root;

    @Test
    void deletesOnlyExpiredUploadTemporaryFilesWhenDeleteModeIsEnabled() throws Exception {
        Path expired = root.resolve(".upload-old.tmp");
        Path recent = root.resolve(".upload-recent.tmp");
        Path unrelated = root.resolve("keep.tmp");
        Files.writeString(expired, "old");
        Files.writeString(recent, "new");
        Files.writeString(unrelated, "keep");
        Files.setLastModifiedTime(expired, FileTime.from(Instant.parse("2026-08-01T00:00:00Z")));
        Files.setLastModifiedTime(unrelated, FileTime.from(Instant.parse("2026-08-01T00:00:00Z")));
        MaintenanceProperties properties = properties(false);

        MaintenanceTaskReport report = service(properties).run();

        assertThat(report.scanned()).isEqualTo(1);
        assertThat(report.affected()).isEqualTo(1);
        assertThat(expired).doesNotExist();
        assertThat(recent).exists();
        assertThat(unrelated).exists();
    }

    @Test
    void dryRunLeavesExpiredTemporaryFilesInPlace() throws Exception {
        Path expired = root.resolve(".upload-old.tmp");
        Files.writeString(expired, "old");
        Files.setLastModifiedTime(expired, FileTime.from(Instant.parse("2026-08-01T00:00:00Z")));

        MaintenanceTaskReport report = service(properties(true)).run();

        assertThat(report.dryRun()).isTrue();
        assertThat(report.scanned()).isEqualTo(1);
        assertThat(report.affected()).isZero();
        assertThat(expired).exists();
    }

    private TempFileMaintenanceService service(MaintenanceProperties properties) {
        FileStorageProperties fileProperties = new FileStorageProperties();
        fileProperties.setLocalRoot(root.toString());
        return new TempFileMaintenanceService(fileProperties, properties,
                Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneId.of("UTC")));
    }

    private static MaintenanceProperties properties(boolean dryRun) {
        MaintenanceProperties properties = new MaintenanceProperties();
        properties.getTempFiles().setDryRun(dryRun);
        properties.getTempFiles().setRetentionMs(1_000L);
        properties.getTempFiles().setBatchSize(10);
        return properties;
    }
}
