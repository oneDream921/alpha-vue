package io.github.onedream921.alphavue.modules.system.service;

import org.springframework.context.annotation.Profile;
import io.github.onedream921.alphavue.framework.redis.RedisPhysicalKey;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 使用 Redis 保存业务参数值。
 */
@Component
@Profile("!test")
class RedisConfigCacheStore implements ConfigCacheStore {
    private static final String CACHE_NAME = "alpha:system:cache:config";
    private final Cache cache;

    RedisConfigCacheStore(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) throw new IllegalStateException("配置缓存未注册");
    }

    public String get(String configKey) {
        return cache.get(key(configKey), String.class);
    }

    public void put(String configKey, String configValue) {
        cache.put(key(configKey), configValue);
    }

    public void evict(String configKey) {
        cache.evict(key(configKey));
    }

    private static String key(String configKey) {
        return RedisPhysicalKey.forIdentifier("system", "config", configKey).value();
    }
}
