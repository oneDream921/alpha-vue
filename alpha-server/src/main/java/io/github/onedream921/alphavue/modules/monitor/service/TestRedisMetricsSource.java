package io.github.onedream921.alphavue.modules.monitor.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 测试环境的无外部依赖占位来源。
 */
@Component
@Profile("test")
class TestRedisMetricsSource implements RedisMetricsSource {
    @Override
    public RedisInfoSnapshot read() {
        return new RedisInfoSnapshot(null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, Map.of());
    }
}
