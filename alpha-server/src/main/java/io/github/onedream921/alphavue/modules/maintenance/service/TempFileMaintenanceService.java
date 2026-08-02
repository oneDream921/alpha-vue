package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Removes stale local upload temporary files with a hard batch limit.
 */
@Service
public class TempFileMaintenanceService {

    private final FileStorageProperties fileProperties;
    private final MaintenanceProperties properties;
    private final Clock clock;

    @Autowired
    public TempFileMaintenanceService(FileStorageProperties fileProperties, MaintenanceProperties properties) {
        this(fileProperties, properties, Clock.systemUTC());
    }

    TempFileMaintenanceService(FileStorageProperties fileProperties, MaintenanceProperties properties, Clock clock) {
        this.fileProperties = fileProperties;
        this.properties = properties;
        this.clock = clock;
    }

    public MaintenanceTaskReport run() {
        MaintenanceProperties.TempFileCleanup config = properties.getTempFiles();
        if (!config.isEnabled()) {
            return MaintenanceTaskReport.skipped("temp-file-cleanup", "disabled");
        }
        Path root = Path.of(fileProperties.getLocalRoot()).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            return new MaintenanceTaskReport("temp-file-cleanup", true, config.isDryRun(), 0, 0, 0, "OK",
                    "local root does not exist");
        }
        int batchSize = properties.safeBatchSize(config.getBatchSize());
        Instant cutoff = clock.instant().minus(Duration.ofMillis(Math.max(1L, config.getRetentionMs())));
        List<Path> candidates = candidates(root, cutoff, batchSize);
        int deleted = 0;
        if (!config.isDryRun()) {
            for (Path candidate : candidates) {
                try {
                    if (Files.deleteIfExists(candidate)) {
                        deleted++;
                    }
                } catch (IOException ignored) {
                    // Keep moving within the bounded batch; the report remains conservative.
                }
            }
        }
        return new MaintenanceTaskReport("temp-file-cleanup", true, config.isDryRun(), candidates.size(), deleted,
                Math.max(0, candidates.size() - deleted), "OK", "cutoff=" + cutoff);
    }

    private static List<Path> candidates(Path root, Instant cutoff, int batchSize) {
        List<Path> result = new ArrayList<>(batchSize);
        try (var stream = Files.walk(root, 4)) {
            stream.sorted(Comparator.naturalOrder())
                    .filter(path -> result.size() < batchSize)
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(TempFileMaintenanceService::isUploadTemporaryFile)
                    .filter(path -> olderThan(path, cutoff))
                    .forEach(result::add);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan local upload temporary files", exception);
        }
        return List.copyOf(result);
    }

    private static boolean isUploadTemporaryFile(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(".upload-") && name.endsWith(".tmp");
    }

    private static boolean olderThan(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff);
        } catch (IOException exception) {
            return false;
        }
    }
}
