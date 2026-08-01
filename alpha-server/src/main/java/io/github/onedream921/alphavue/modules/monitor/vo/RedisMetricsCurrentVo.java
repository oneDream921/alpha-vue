package io.github.onedream921.alphavue.modules.monitor.vo;

/**
 * Redis 指标当前值。
 */
public record RedisMetricsCurrentVo(
        String redisVersion,
        Long uptimeSeconds,
        Long usedMemoryBytes,
        Long usedMemoryRssBytes,
        Long usedMemoryPeakBytes,
        Long maxMemoryBytes,
        Long totalSystemMemoryBytes,
        Double memoryFragmentationRatio,
        Long connectedClients,
        Long blockedClients,
        Long totalConnectionsReceived,
        Long rejectedConnections,
        Long totalCommandsProcessed,
        Long instantaneousOpsPerSecond,
        Long keyspaceHits,
        Long keyspaceMisses) {
}
