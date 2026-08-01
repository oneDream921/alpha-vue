package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisCommandMetricVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisMetricsCurrentVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisMetricsTrendPointVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisMetricsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前应用实例内的 Redis 指标采样器。
 */
@Component
public class RedisMetricsSampler {
    private static final Logger log = LoggerFactory.getLogger(RedisMetricsSampler.class);
    private static final int TOP_COMMANDS = 10;

    private final RedisMetricsSource source;
    private final RedisManagementProperties properties;
    private final Clock clock;
    private final Object monitor = new Object();
    private final Deque<Sample> samples = new ArrayDeque<>();
    private Instant lastAttemptAt;
    private Instant lastSuccessAt;
    private int consecutiveFailures;
    private RedisInfoSnapshot latest;
    private List<RedisCommandMetricVo> latestCommandMetrics = List.of();
    private Map<String, RedisCommandSnapshot> previousCommands = Map.of();
    private Long previousTotalCommands;
    private Long previousUptimeSeconds;
    private Instant previousCapturedAt;

    @Autowired
    public RedisMetricsSampler(RedisMetricsSource source, RedisManagementProperties properties) {
        this(source, properties, Clock.systemUTC());
    }

    RedisMetricsSampler(RedisMetricsSource source, RedisManagementProperties properties, Clock clock) {
        this.source = source;
        this.properties = properties;
        this.clock = clock;
    }

    @jakarta.annotation.PostConstruct
    void sampleOnStartup() {
        if (properties.isMetricsEnabled()) {
            sampleNow();
        }
    }

    @Scheduled(fixedDelayString = "${alpha.monitor.redis.metrics-sample-interval-ms:60000}",
            initialDelayString = "${alpha.monitor.redis.metrics-sample-interval-ms:60000}")
    void scheduledSample() {
        sampleNow();
    }

    public void sampleNow() {
        Instant attempt = clock.instant();
        synchronized (monitor) {
            lastAttemptAt = attempt;
        }
        if (!properties.isMetricsEnabled()) {
            return;
        }
        try {
            RedisInfoSnapshot snapshot = source.read();
            synchronized (monitor) {
                addSuccess(attempt, snapshot);
            }
        } catch (RuntimeException exception) {
            synchronized (monitor) {
                consecutiveFailures++;
            }
            log.warn("Redis 指标采样失败，类型={}", exception.getClass().getSimpleName());
        }
    }

    public RedisMetricsVo snapshot() {
        Instant now = clock.instant();
        synchronized (monitor) {
            trimSamples(now);
            List<RedisMetricsTrendPointVo> trend = samples.stream().map(Sample::trend).toList();
            return new RedisMetricsVo(properties.isMetricsEnabled(), status(now), lastAttemptAt, lastSuccessAt,
                    consecutiveFailures, intervalSeconds(), retentionHours(), maxSamples(),
                    latest == null ? null : current(latest), latestCommandMetrics, trend);
        }
    }

    private void addSuccess(Instant capturedAt, RedisInfoSnapshot snapshot) {
        boolean counterReset = counterReset(snapshot);
        latestCommandMetrics = commandMetrics(snapshot.commands(), counterReset, capturedAt);
        latest = snapshot;
        lastSuccessAt = capturedAt;
        consecutiveFailures = 0;
        previousCommands = snapshot.commands();
        previousTotalCommands = snapshot.totalCommandsProcessed();
        previousUptimeSeconds = snapshot.uptimeSeconds();
        previousCapturedAt = capturedAt;
        samples.addLast(new Sample(capturedAt, snapshot));
        trimSamples(capturedAt);
    }

    private boolean counterReset(RedisInfoSnapshot snapshot) {
        return previousUptimeSeconds != null && snapshot.uptimeSeconds() != null
                && snapshot.uptimeSeconds() < previousUptimeSeconds
                || previousTotalCommands != null && snapshot.totalCommandsProcessed() != null
                && snapshot.totalCommandsProcessed() < previousTotalCommands;
    }

    private void trimSamples(Instant now) {
        int maxSamples = maxSamples();
        Instant oldest = now.minusMillis(Math.max(properties.getMetricsRetentionMs(), 0));
        while (samples.size() > maxSamples || (!samples.isEmpty() && samples.peekFirst().capturedAt().isBefore(oldest))) {
            samples.removeFirst();
        }
    }

    private String status(Instant now) {
        if (!properties.isMetricsEnabled()) return "DISABLED";
        if (lastAttemptAt == null) return "COLLECTING";
        if (lastSuccessAt == null) return consecutiveFailures > 0 ? "DEGRADED" : "COLLECTING";
        long staleAfter = Math.max(intervalSeconds() * 2, 120L);
        if (Duration.between(lastSuccessAt, now).getSeconds() >= staleAfter) return "STALE";
        if (consecutiveFailures == 0) return "HEALTHY";
        return "DEGRADED";
    }

    private List<RedisCommandMetricVo> commandMetrics(Map<String, RedisCommandSnapshot> commands,
                                                      boolean counterReset, Instant capturedAt) {
        List<RedisCommandMetricVo> result = new ArrayList<>();
        for (Map.Entry<String, RedisCommandSnapshot> entry : commands.entrySet()) {
            RedisCommandSnapshot current = entry.getValue();
            RedisCommandSnapshot previous = previousCommands.get(entry.getKey());
            Long intervalCalls = counterReset ? null : delta(current.calls(), previous == null ? null : previous.calls());
            Double callsPerSecond = callsPerSecond(intervalCalls, capturedAt);
            result.add(new RedisCommandMetricVo(entry.getKey(), current.calls(), intervalCalls, callsPerSecond,
                    current.rejectedCalls(), current.failedCalls(), current.usec(), current.usecPerCall()));
        }
        result.sort(Comparator.comparingLong((RedisCommandMetricVo item) -> item.calls() == null ? 0 : item.calls())
                .reversed().thenComparing(RedisCommandMetricVo::command));
        return result.size() > TOP_COMMANDS ? List.copyOf(result.subList(0, TOP_COMMANDS)) : List.copyOf(result);
    }

    private Double callsPerSecond(Long intervalCalls, Instant capturedAt) {
        if (intervalCalls == null || previousCapturedAt == null) return null;
        long seconds = Math.max(1L, Duration.between(previousCapturedAt, capturedAt).toMillis()) / 1_000L;
        return intervalCalls.doubleValue() / Math.max(1L, seconds);
    }

    private static Long delta(Long current, Long previous) {
        if (current == null || previous == null || current < previous) return null;
        return current - previous;
    }

    private RedisMetricsCurrentVo current(RedisInfoSnapshot snapshot) {
        return new RedisMetricsCurrentVo(snapshot.redisVersion(), snapshot.uptimeSeconds(), snapshot.usedMemoryBytes(),
                snapshot.usedMemoryRssBytes(), snapshot.usedMemoryPeakBytes(), snapshot.maxMemoryBytes(),
                snapshot.totalSystemMemoryBytes(), snapshot.memoryFragmentationRatio(), snapshot.connectedClients(),
                snapshot.blockedClients(), snapshot.totalConnectionsReceived(), snapshot.rejectedConnections(),
                snapshot.totalCommandsProcessed(), snapshot.instantaneousOpsPerSecond(), snapshot.keyspaceHits(),
                snapshot.keyspaceMisses());
    }

    private long intervalSeconds() {
        return Math.max(1L, properties.getMetricsSampleIntervalMs() / 1_000L);
    }

    private long retentionHours() {
        return Math.max(1L, properties.getMetricsRetentionMs() / (60 * 60 * 1_000L));
    }

    private int maxSamples() {
        return Math.max(1, Math.min(properties.getMetricsMaxSamples(), 1_440));
    }

    private record Sample(Instant capturedAt, RedisInfoSnapshot snapshot) {
        private RedisMetricsTrendPointVo trend() {
            return new RedisMetricsTrendPointVo(capturedAt, snapshot.usedMemoryBytes(), snapshot.usedMemoryRssBytes(),
                    snapshot.connectedClients(), snapshot.blockedClients(), snapshot.instantaneousOpsPerSecond(),
                    snapshot.totalCommandsProcessed(), snapshot.rejectedConnections());
        }
    }
}
