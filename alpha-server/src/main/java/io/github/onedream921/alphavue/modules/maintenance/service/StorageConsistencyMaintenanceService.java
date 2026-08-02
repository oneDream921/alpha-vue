package io.github.onedream921.alphavue.modules.maintenance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.file.entity.SysFile;
import io.github.onedream921.alphavue.modules.file.mapper.SysFileMapper;
import io.github.onedream921.alphavue.modules.file.storage.StorageProvider;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reports file metadata records whose storage object cannot be opened.
 */
@Service
public class StorageConsistencyMaintenanceService {

    private final SysFileMapper fileMapper;
    private final Map<String, StorageProvider> providers;
    private final MaintenanceProperties properties;

    public StorageConsistencyMaintenanceService(SysFileMapper fileMapper, List<StorageProvider> providers,
            MaintenanceProperties properties) {
        this.fileMapper = fileMapper;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> normalize(provider.name()), provider -> provider));
        this.properties = properties;
    }

    public MaintenanceTaskReport run() {
        MaintenanceProperties.StorageConsistency config = properties.getStorageConsistency();
        if (!config.isEnabled()) {
            return MaintenanceTaskReport.skipped("storage-consistency", "disabled");
        }
        int batchSize = properties.safeBatchSize(config.getBatchSize());
        Page<SysFile> page = fileMapper.selectPage(new Page<>(1, batchSize),
                new LambdaQueryWrapper<SysFile>().orderByDesc(SysFile::getId));
        int missing = 0;
        int skipped = 0;
        for (SysFile file : page.getRecords()) {
            StorageProvider provider = providers.get(normalize(file.getStorageProvider()));
            if (provider == null) {
                skipped++;
                continue;
            }
            try (var ignored = provider.open(file.getObjectKey())) {
                // Opening is enough to verify the object is addressable.
            } catch (IOException exception) {
                missing++;
            }
        }
        return new MaintenanceTaskReport("storage-consistency", true, true, page.getRecords().size(), missing,
                skipped, "OK", "reported missing objects only");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
