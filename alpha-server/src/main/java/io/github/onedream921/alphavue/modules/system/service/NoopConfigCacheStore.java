package io.github.onedream921.alphavue.modules.system.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 测试环境禁用外部 Redis 依赖。
 */
@Component
@Profile("test")
class NoopConfigCacheStore implements ConfigCacheStore {
    private final ConcurrentMap<String, String> values = new ConcurrentHashMap<>();

    public String get(String configKey) { return values.get(configKey); }
    public void put(String configKey, String configValue) { values.put(configKey, configValue); }
    public void evict(String configKey) { values.remove(configKey); }
}
