package io.github.onedream921.alphavue.modules.maintenance.service;

import cn.dev33.satoken.dao.SaTokenDao;
import io.github.onedream921.alphavue.framework.redis.RedissonSaTokenDao;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.springframework.stereotype.Service;

/**
 * Repairs stale Sa-Token Redis search indexes when Redisson storage is active.
 */
@Service
public class SessionIndexMaintenanceService {

    private final SaTokenDao saTokenDao;
    private final MaintenanceProperties properties;

    public SessionIndexMaintenanceService(SaTokenDao saTokenDao, MaintenanceProperties properties) {
        this.saTokenDao = saTokenDao;
        this.properties = properties;
    }

    public MaintenanceTaskReport run() {
        MaintenanceProperties.SessionIndex config = properties.getSessions();
        if (!config.isEnabled()) {
            return MaintenanceTaskReport.skipped("session-index-repair", "disabled");
        }
        if (!(saTokenDao instanceof RedissonSaTokenDao redissonSaTokenDao)) {
            return MaintenanceTaskReport.skipped("session-index-repair", "non-redisson session store");
        }
        RedissonSaTokenDao.SessionIndexRepairResult result = redissonSaTokenDao.repairStaleIndexes(
                properties.safeBatchSize(config.getBatchSize()), config.isDryRun());
        return new MaintenanceTaskReport("session-index-repair", true, config.isDryRun(), result.scanned(),
                result.removed(), result.stale() - result.removed(), "OK", "stale=" + result.stale());
    }
}
