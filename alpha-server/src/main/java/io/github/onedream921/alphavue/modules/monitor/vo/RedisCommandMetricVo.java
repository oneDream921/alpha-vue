package io.github.onedream921.alphavue.modules.monitor.vo;

/**
 * Redis 单个命令的安全统计。
 */
public record RedisCommandMetricVo(
        String command,
        Long calls,
        Long intervalCalls,
        Double callsPerSecond,
        Long rejectedCalls,
        Long failedCalls,
        Long usec,
        Double usecPerCall) {
}
