package io.github.onedream921.alphavue.modules.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
import io.github.onedream921.alphavue.framework.redis.RedisPhysicalKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 使用 Redis 保存启用字典项列表。
 */
@Component
@Profile("!test")
class RedisDictCacheStore implements DictCacheStore {
    private static final String CACHE_NAME = "alpha:system:cache:dictionary";

    private static final Logger log = LoggerFactory.getLogger(RedisDictCacheStore.class);

    private final Cache cache;
    private final ObjectMapper objectMapper;
    private final JavaType itemListType;

    RedisDictCacheStore(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) throw new IllegalStateException("字典缓存未注册");
        this.objectMapper = JsonMapper.builder().build();
        this.itemListType = this.objectMapper.getTypeFactory()
                .constructCollectionType(List.class, EnabledDictItemVo.class);
    }

    @Override
    public List<EnabledDictItemVo> get(String typeCode) {
        String json = cache.get(key(typeCode), String.class);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, itemListType);
        } catch (JsonProcessingException exception) {
            log.warn("字典缓存反序列化失败，已删除该类型缓存");
            evict(typeCode);
            return null;
        }
    }

    @Override
    public void put(String typeCode, List<EnabledDictItemVo> items) {
        try {
            cache.put(key(typeCode), objectMapper.writeValueAsString(items));
        } catch (JsonProcessingException exception) {
            log.warn("字典缓存序列化失败，已跳过写入");
        }
    }

    @Override
    public void evict(String typeCode) {
        cache.evict(key(typeCode));
    }

    private static String key(String typeCode) {
        return RedisPhysicalKey.forIdentifier("system", "dict", typeCode).value();
    }
}
