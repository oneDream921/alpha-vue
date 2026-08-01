package io.github.onedream921.alphavue.modules.monitor.vo;

import java.time.Instant;

/**
 * Redis 指标趋势点。
 */
public record RedisMetricsTrendPointVo(
        Instant capturedAt,
        Long usedMemoryBytes,
        Long usedMemoryRssBytes,
        Long connectedClients,
        Long blockedClients,
        Long instantaneousOpsPerSecond,
        Long totalCommandsProcessed,
        Long rejectedConnections) {
}
