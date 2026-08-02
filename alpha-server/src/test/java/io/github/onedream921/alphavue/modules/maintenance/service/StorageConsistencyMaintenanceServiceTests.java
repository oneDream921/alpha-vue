package io.github.onedream921.alphavue.modules.maintenance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.file.entity.SysFile;
import io.github.onedream921.alphavue.modules.file.mapper.SysFileMapper;
import io.github.onedream921.alphavue.modules.file.storage.StorageProvider;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageConsistencyMaintenanceServiceTests {

    @Test
    void reportsMissingObjectsAndUnknownProvidersWithoutDeletingAnything() {
        SysFileMapper mapper = mock(SysFileMapper.class);
        Page<SysFile> page = new Page<>(1, 10);
        page.setRecords(List.of(file("local", "present.txt"), file("local", "missing.txt"),
                file("unknown", "ignored.txt")));
        when(mapper.selectPage(any(), any())).thenReturn(page);
        StorageProvider provider = new StorageProvider() {
            @Override public String name() { return "local"; }
            @Override public void store(String key, InputStream input, String contentType) { }
            @Override public void delete(String key) { throw new AssertionError("must not delete"); }
            @Override public InputStream open(String key) throws IOException {
                if ("missing.txt".equals(key)) {
                    throw new IOException("missing");
                }
                return new ByteArrayInputStream(new byte[] {1});
            }
            @Override public String publicUrl(String key) { return "/uploads/" + key; }
        };

        MaintenanceTaskReport report = new StorageConsistencyMaintenanceService(mapper, List.of(provider),
                new MaintenanceProperties()).run();

        assertThat(report.scanned()).isEqualTo(3);
        assertThat(report.affected()).isEqualTo(1);
        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.dryRun()).isTrue();
    }

    private static SysFile file(String provider, String key) {
        SysFile file = new SysFile();
        file.setStorageProvider(provider);
        file.setObjectKey(key);
        return file;
    }
}
