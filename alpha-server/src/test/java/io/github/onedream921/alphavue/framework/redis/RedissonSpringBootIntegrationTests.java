package io.github.onedream921.alphavue.framework.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Redisson Client、Spring Cache 与 Spring Boot 4 应用上下文共存。
 */
@SpringBootTest
@ActiveProfiles("p1-03")
@EnabledIfEnvironmentVariable(named = "REDIS_INTEGRATION_TESTS", matches = "true")
class RedissonSpringBootIntegrationTests {

    private static final String KEY = "alpha:test:boot:" + UUID.randomUUID();
    private static RedissonClient client;

    @Autowired
    void captureClient(RedissonClient redissonClient) {
        client = redissonClient;
    }

    @Autowired
    void verifyCacheManager(CacheManager cacheManager) {
        assertThat(cacheManager.getCache("alpha:system:cache:dictionary")).isNotNull();
    }

    @Test
    void createsRedissonClientInsideSpringBoot4Context() {
        client.getBucket(KEY).set("boot-ok");
        assertThat(client.<String>getBucket(KEY).get()).isEqualTo("boot-ok");
    }

    @AfterAll
    static void cleanup() {
        if (client != null) {
            client.getKeys().delete(KEY);
        }
    }
}
