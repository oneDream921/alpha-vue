package io.github.onedream921.alphavue.framework.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redisson 连接与缓存运行参数。
 */
@ConfigurationProperties(prefix = "alpha.redis.redisson")
public record RedissonProperties(
        String host,
        int port,
        String password,
        int database,
        Duration timeout,
        Duration connectTimeout,
        int connectionPoolSize,
        int connectionMinimumIdleSize,
        Duration retryInterval,
        int retryAttempts,
        Duration cacheTtl) {
}
