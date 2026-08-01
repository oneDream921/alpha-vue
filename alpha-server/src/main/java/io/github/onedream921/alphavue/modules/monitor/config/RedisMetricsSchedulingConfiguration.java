package io.github.onedream921.alphavue.modules.monitor.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 仅在指标开关打开时启用 Redis 指标采样调度。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "alpha.monitor.redis", name = "metrics-enabled", havingValue = "true")
public class RedisMetricsSchedulingConfiguration {
}
