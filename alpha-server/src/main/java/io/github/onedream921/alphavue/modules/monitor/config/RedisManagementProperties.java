package io.github.onedream921.alphavue.modules.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 运维台安全配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.monitor.redis")
public class RedisManagementProperties {

    /**
     * 是否启用 Redis INFO 指标采样。
     */
    private boolean metricsEnabled = true;

    /**
     * 指标采样间隔，默认一分钟。
     */
    private long metricsSampleIntervalMs = 60_000L;

    /**
     * 成功采样在内存中的最长保留时间。
     */
    private long metricsRetentionMs = 24 * 60 * 60 * 1_000L;

    /**
     * 单实例最多保留的成功采样数量。
     */
    private int metricsMaxSamples = 1_440;

    /**
     * 是否对验证码、会话和疑似密钥等敏感键的值进行脱敏。
     */
    private boolean maskValues = true;
}
