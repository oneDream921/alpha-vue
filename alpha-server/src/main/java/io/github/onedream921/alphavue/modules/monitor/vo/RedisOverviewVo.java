package io.github.onedream921.alphavue.modules.monitor.vo;

import java.util.Map;

/**
 * Redis 受控运维概览
 */
public record RedisOverviewVo(String redisVersion, Long uptimeSeconds, Long usedMemoryBytes,
                              Long connectedClients, Map<String, Long> managedKeyCounts) {
    public RedisOverviewVo {
        managedKeyCounts = Map.copyOf(managedKeyCounts);
    }
}
