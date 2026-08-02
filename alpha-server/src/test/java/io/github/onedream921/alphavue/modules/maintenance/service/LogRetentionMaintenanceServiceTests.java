package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.log.mapper.SysLoginLogMapper;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogRetentionMaintenanceServiceTests {

    @Test
    void dryRunReportsCandidatesWithoutDeletingLogs() {
        SysLoginLogMapper loginMapper = mock(SysLoginLogMapper.class);
        SysOperLogMapper operMapper = mock(SysOperLogMapper.class);
        when(loginMapper.selectExpiredIds(javaTime(), 10)).thenReturn(List.of(1L));
        when(operMapper.selectExpiredHandledIds(javaTime(), 9)).thenReturn(List.of(2L));
        MaintenanceProperties properties = properties(true);

        MaintenanceTaskReport report = service(loginMapper, operMapper, properties).run();

        assertThat(report.dryRun()).isTrue();
        assertThat(report.scanned()).isEqualTo(2);
        assertThat(report.affected()).isZero();
        verify(loginMapper, never()).deleteBatchIds(any());
        verify(operMapper, never()).deleteBatchIds(any());
    }

    @Test
    void deleteModeDeletesOnlyTheSelectedBatchIds() {
        SysLoginLogMapper loginMapper = mock(SysLoginLogMapper.class);
        SysOperLogMapper operMapper = mock(SysOperLogMapper.class);
        when(loginMapper.selectExpiredIds(javaTime(), 10)).thenReturn(List.of(1L));
        when(operMapper.selectExpiredHandledIds(javaTime(), 9)).thenReturn(List.of(2L));
        when(loginMapper.deleteBatchIds(List.of(1L))).thenReturn(1);
        when(operMapper.deleteBatchIds(List.of(2L))).thenReturn(1);
        MaintenanceProperties properties = properties(false);

        MaintenanceTaskReport report = service(loginMapper, operMapper, properties).run();

        assertThat(report.dryRun()).isFalse();
        assertThat(report.scanned()).isEqualTo(2);
        assertThat(report.affected()).isEqualTo(2);
        verify(loginMapper).deleteBatchIds(List.of(1L));
        verify(operMapper).deleteBatchIds(List.of(2L));
    }

    private static LogRetentionMaintenanceService service(SysLoginLogMapper loginMapper, SysOperLogMapper operMapper,
            MaintenanceProperties properties) {
        return new LogRetentionMaintenanceService(loginMapper, operMapper, properties,
                Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneId.of("UTC")));
    }

    private static MaintenanceProperties properties(boolean dryRun) {
        MaintenanceProperties properties = new MaintenanceProperties();
        properties.getLogs().setDryRun(dryRun);
        properties.getLogs().setRetentionDays(30);
        properties.getLogs().setBatchSize(10);
        return properties;
    }

    private static java.time.LocalDateTime javaTime() {
        return java.time.LocalDateTime.ofInstant(Instant.parse("2026-07-03T00:00:00Z"), ZoneId.of("UTC"));
    }
}
