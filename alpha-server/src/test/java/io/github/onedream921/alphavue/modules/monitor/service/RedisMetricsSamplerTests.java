package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisMetricsVo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RedisMetricsSamplerTests {

    @Test
    void keepsBoundedSuccessfulHistoryAndCalculatesNonNegativeCommandDelta() {
        RedisManagementProperties properties = enabledProperties();
        properties.setMetricsMaxSamples(2);
        MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
        Queue<RedisInfoSnapshot> snapshots = new ArrayDeque<>();
        snapshots.add(snapshot(10, 100, 1, 10));
        snapshots.add(snapshot(11, 140, 2, 12));
        snapshots.add(snapshot(12, 90, 3, 14));
        RedisMetricsSampler sampler = new RedisMetricsSampler(() -> snapshots.remove(), properties, clock);

        sampler.sampleNow();
        clock.advance(Duration.ofMinutes(1));
        sampler.sampleNow();
        clock.advance(Duration.ofMinutes(1));
        sampler.sampleNow();

        RedisMetricsVo result = sampler.snapshot();
        assertThat(result.status()).isEqualTo("HEALTHY");
        assertThat(result.trend()).hasSize(2);
        assertThat(result.commands()).singleElement().satisfies(command -> {
            assertThat(command.command()).isEqualTo("get");
            assertThat(command.intervalCalls()).isEqualTo(2L);
            assertThat(command.callsPerSecond()).isEqualTo(2D / 60D);
        });
    }

    @Test
    void failedSamplingPreservesLastSuccessAndReportsDegradedWithoutAddingTrendPoint() {
        RedisManagementProperties properties = enabledProperties();
        AtomicInteger calls = new AtomicInteger();
        RedisMetricsSampler sampler = new RedisMetricsSampler(() -> {
            if (calls.getAndIncrement() == 0) return snapshot(10, 100, 1, 10);
            throw new IllegalStateException("connection details must not be returned");
        }, properties, Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneId.of("UTC")));

        sampler.sampleNow();
        sampler.sampleNow();

        RedisMetricsVo result = sampler.snapshot();
        assertThat(result.status()).isEqualTo("DEGRADED");
        assertThat(result.consecutiveFailures()).isEqualTo(1);
        assertThat(result.current().usedMemoryBytes()).isEqualTo(100L);
        assertThat(result.current().totalSystemMemoryBytes()).isEqualTo(1_000L);
        assertThat(result.trend()).hasSize(1);
    }

    @Test
    void disabledSamplingDoesNotCallRedisSource() {
        RedisManagementProperties properties = enabledProperties();
        properties.setMetricsEnabled(false);
        AtomicInteger calls = new AtomicInteger();
        RedisMetricsSampler sampler = new RedisMetricsSampler(() -> {
            calls.incrementAndGet();
            return snapshot(10, 100, 1, 10);
        }, properties, Clock.systemUTC());

        sampler.sampleNow();

        assertThat(calls).hasValue(0);
        assertThat(sampler.snapshot().status()).isEqualTo("DISABLED");
    }

    @Test
    void evictsExpiredTrendAndReportsStaleWhenSamplingStops() {
        RedisManagementProperties properties = enabledProperties();
        properties.setMetricsRetentionMs(Duration.ofMinutes(1).toMillis());
        MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
        RedisMetricsSampler sampler = new RedisMetricsSampler(() -> snapshot(10, 100, 1, 10), properties, clock);

        sampler.sampleNow();
        clock.advance(Duration.ofMinutes(2));

        RedisMetricsVo result = sampler.snapshot();
        assertThat(result.status()).isEqualTo("STALE");
        assertThat(result.trend()).isEmpty();
        assertThat(result.current().usedMemoryBytes()).isEqualTo(100L);
    }

    private static RedisManagementProperties enabledProperties() {
        RedisManagementProperties properties = new RedisManagementProperties();
        properties.setMetricsEnabled(true);
        properties.setMetricsSampleIntervalMs(60_000L);
        properties.setMetricsRetentionMs(Duration.ofHours(24).toMillis());
        return properties;
    }

    private static RedisInfoSnapshot snapshot(long uptime, long memory, long connected, long calls) {
        return new RedisInfoSnapshot("7.4.1", uptime, memory, memory + 10, memory + 20, 0L, memory * 10, 1.1D,
                connected, 0L, 100L, 0L, calls, 2L, 10L, 1L,
                Map.of("get", new RedisCommandSnapshot(calls, 20L, 2D, 0L, 0L)));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
