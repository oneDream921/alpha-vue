package io.github.onedream921.alphavue.modules.maintenance.service;

import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import io.github.onedream921.alphavue.modules.maintenance.config.MaintenanceProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionIndexMaintenanceServiceTests {

    @Test
    void skipsRepairWhenSessionStoreIsNotRedissonBacked() {
        MaintenanceTaskReport report = new SessionIndexMaintenanceService(new SaTokenDaoDefaultImpl(),
                new MaintenanceProperties()).run();

        assertThat(report.status()).isEqualTo("SKIPPED");
        assertThat(report.message()).isEqualTo("non-redisson session store");
    }
}
