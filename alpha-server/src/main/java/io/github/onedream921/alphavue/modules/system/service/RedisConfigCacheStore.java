package io.github.onedream921.alphavue.modules.system.service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 使用 Redis 保存业务参数值。
 */
@Component
@Profile("!test")
class RedisConfigCacheStore implements ConfigCacheStore {
    private static final String KEY_PREFIX = "system:config:";
    private final StringRedisTemplate redisTemplate;
    RedisConfigCacheStore(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }
    public String get(String configKey) { return redisTemplate.opsForValue().get(KEY_PREFIX + configKey); }
    public void put(String configKey, String configValue) { redisTemplate.opsForValue().set(KEY_PREFIX + configKey, configValue); }
    public void evict(String configKey) { redisTemplate.delete(KEY_PREFIX + configKey); }
}
