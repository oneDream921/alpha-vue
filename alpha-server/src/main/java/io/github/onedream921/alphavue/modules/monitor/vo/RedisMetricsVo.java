package io.github.onedream921.alphavue.modules.monitor.vo;

import java.time.Instant;
import java.util.List;

/**
 * Redis 当前实例指标响应。
 */
public record RedisMetricsVo(
        boolean enabled,
        String status,
        Instant lastAttemptAt,
        Instant lastSuccessAt,
        int consecutiveFailures,
        long sampleIntervalSeconds,
        long retentionHours,
        int maxSamples,
        RedisMetricsCurrentVo current,
        List<RedisCommandMetricVo> commands,
        List<RedisMetricsTrendPointVo> trend) {

    public RedisMetricsVo {
        commands = List.copyOf(commands);
        trend = List.copyOf(trend);
    }
}
