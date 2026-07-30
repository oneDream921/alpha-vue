package io.github.onedream921.alphavue.framework.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redisson Client 和 Spring Cache 配置。
 *
 * <p>业务 Redis 访问统一通过 Redisson，并限定在受控 alpha 命名空间。</p>
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient(RedissonProperties properties, org.redisson.client.codec.StringCodec stringCodec) {
        Config config = new Config().setCodec(stringCodec);
        var server = config.useSingleServer()
                .setAddress("redis://" + properties.host() + ':' + properties.port())
                .setDatabase(properties.database())
                .setTimeout((int) properties.timeout().toMillis())
                .setConnectTimeout((int) properties.connectTimeout().toMillis())
                .setRetryInterval((int) properties.retryInterval().toMillis())
                .setRetryAttempts(properties.retryAttempts())
                .setConnectionPoolSize(properties.connectionPoolSize())
                .setConnectionMinimumIdleSize(properties.connectionMinimumIdleSize());
        if (properties.password() != null && !properties.password().isBlank()) {
            server.setPassword(properties.password());
        }
        return Redisson.create(config);
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient, Codec redissonCacheCodec, RedissonProperties properties) {
        Map<String, CacheConfig> caches = new LinkedHashMap<>();
        long ttl = properties.cacheTtl().toMillis();
        caches.put("alpha:system:cache:dictionary", new CacheConfig(ttl, 0));
        caches.put("alpha:system:cache:config", new CacheConfig(ttl, 0));
        RedissonSpringCacheManager manager = new RedissonSpringCacheManager(redissonClient, caches, redissonCacheCodec);
        manager.setAllowNullValues(false);
        manager.setTransactionAware(true);
        return manager;
    }
}
