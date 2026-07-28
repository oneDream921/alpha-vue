package io.github.onedream921.alphavue.modules.system.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 测试环境禁用外部 Redis 依赖。
 */
@Component
@Profile("test")
class NoopConfigCacheStore implements ConfigCacheStore {
    public String get(String configKey) { return null; }
    public void put(String configKey, String configValue) { }
    public void evict(String configKey) { }
}
