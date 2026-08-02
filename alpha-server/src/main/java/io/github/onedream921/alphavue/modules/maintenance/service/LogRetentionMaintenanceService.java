package io.github.onedream921.alphavue.modules.maintenance.service;

import io.github.onedream921.alphavue.modules.log.mapper.SysLoginLogMapper;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Applies bounded retention to login and handled operation logs.
 */
@Service
public class LogRetentionMaintenanceService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;
    private final MaintenanceProperties properties;
    private final Clock clock;

    @Autowired
    public LogRetentionMaintenanceService(SysLoginLogMapper loginLogMapper, SysOperLogMapper operLogMapper,
            MaintenanceProperties properties) {
        this(loginLogMapper, operLogMapper, properties, Clock.systemDefaultZone());
    }

    LogRetentionMaintenanceService(SysLoginLogMapper loginLogMapper, SysOperLogMapper operLogMapper,
            MaintenanceProperties properties, Clock clock) {
        this.loginLogMapper = loginLogMapper;
        this.operLogMapper = operLogMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public MaintenanceTaskReport run() {
        MaintenanceProperties.LogCleanup config = properties.getLogs();
        if (!config.isEnabled()) {
            return MaintenanceTaskReport.skipped("log-retention", "disabled");
        }
        int batchSize = properties.safeBatchSize(config.getBatchSize());
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(Duration.ofDays(Math.max(1, config.getRetentionDays())));
        List<Long> loginIds = loginLogMapper.selectExpiredIds(cutoff, batchSize);
        int remaining = Math.max(0, batchSize - loginIds.size());
        List<Long> operationIds = remaining == 0 ? List.of() : operLogMapper.selectExpiredHandledIds(cutoff, remaining);
        int affected = 0;
        if (!config.isDryRun()) {
            if (!loginIds.isEmpty()) {
                affected += loginLogMapper.deleteBatchIds(loginIds);
            }
            if (!operationIds.isEmpty()) {
                affected += operLogMapper.deleteBatchIds(operationIds);
            }
        }
        int candidates = loginIds.size() + operationIds.size();
        return new MaintenanceTaskReport("log-retention", true, config.isDryRun(), candidates, affected, 0,
                "OK", "cutoff=" + cutoff);
    }
}
