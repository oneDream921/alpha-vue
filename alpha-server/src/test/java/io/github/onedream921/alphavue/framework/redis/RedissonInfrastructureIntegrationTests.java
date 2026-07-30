package io.github.onedream921.alphavue.framework.redis;

import cn.dev33.satoken.session.SaSession;
import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.Cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 使用 REDIS_INTEGRATION_TESTS=true 显式开启的独立 Redis 验证。
 *
 * <p>测试只访问唯一 alpha 命名空间，并按已知键逐项删除，不清理共享数据库。</p>
 */
@EnabledIfEnvironmentVariable(named = "REDIS_INTEGRATION_TESTS", matches = "true")
class RedissonInfrastructureIntegrationTests {

    private static final String CACHE_NAME = "alpha:test:cache:" + UUID.randomUUID();
    private static final String NAMESPACE = "alpha:test:core:" + UUID.randomUUID();
    private static final RedisKey SA_TOKEN_KEY = RedisKey.of("test", "sa-token", UUID.randomUUID().toString());
    private static RedissonClient client;
    private static Codec cacheCodec;
    private static Codec saTokenCodec;
    private static RedissonSpringCacheManager cacheManager;

    @BeforeAll
    static void startClient() {
        RedissonCodecRegistry registry = new RedissonCodecRegistry();
        cacheCodec = registry.redissonCacheCodec();
        saTokenCodec = registry.redissonSaTokenCodec();
        Config config = new Config().setCodec(StringCodec.INSTANCE);
        var server = config.useSingleServer()
                .setAddress("redis://" + env("REDIS_TEST_HOST", "127.0.0.1") + ':' + envInt("REDIS_TEST_PORT", 6379))
                .setDatabase(envInt("REDIS_TEST_DATABASE", 15))
                .setTimeout(1500)
                .setConnectTimeout(1500)
                .setRetryAttempts(1);
        String password = System.getenv("REDIS_TEST_PASSWORD");
        if (password != null && !password.isBlank()) {
            server.setPassword(password);
        }
        client = Redisson.create(config);
        Map<String, CacheConfig> configs = new LinkedHashMap<>();
        configs.put(CACHE_NAME, new CacheConfig(2_000, 0));
        cacheManager = new RedissonSpringCacheManager(client, configs, cacheCodec);
        cacheManager.setAllowNullValues(false);
    }

    @AfterAll
    static void stopClient() {
        if (cacheManager != null) {
            cacheManager.getCache(CACHE_NAME).clear();
        }
        if (client != null) {
            client.getKeys().delete(NAMESPACE + ":string", NAMESPACE + ":object", SA_TOKEN_KEY.value(), NAMESPACE + ":unregistered", NAMESPACE + ":counter");
            client.shutdown();
        }
    }

    @Test
    void supportsStringCodecTtlAndAtomicOperation() throws InterruptedException {
        client.getBucket(NAMESPACE + ":string").set("value", 2, TimeUnit.SECONDS);
        assertThat(client.<String>getBucket(NAMESPACE + ":string").get()).isEqualTo("value");
        assertThat(client.getBucket(NAMESPACE + ":string").remainTimeToLive()).isBetween(1_000L, 2_000L);
        assertThat(client.getAtomicLong(NAMESPACE + ":counter").incrementAndGet()).isEqualTo(1L);
        assertThat(client.getAtomicLong(NAMESPACE + ":counter").incrementAndGet()).isEqualTo(2L);
        Thread.sleep(2_100L);
        assertThat(client.getBucket(NAMESPACE + ":string").isExists()).isFalse();
    }

    @Test
    void usesExplicitObjectWhitelistsForCacheAndSaTokenCodecs() {
        EnabledDictItemVo item = new EnabledDictItemVo("label", "value", 1, 0);
        client.<EnabledDictItemVo>getBucket(NAMESPACE + ":object", cacheCodec).set(item);
        assertThat(client.<EnabledDictItemVo>getBucket(NAMESPACE + ":object", cacheCodec).get()).isEqualTo(item);
        SaSession session = new SaSession("session-id").setLoginId(1L).setToken("token");
        RedissonSaTokenObjectAdapter adapter = new RedissonSaTokenObjectAdapter(client, saTokenCodec);
        adapter.setObject(SA_TOKEN_KEY, session, java.time.Duration.ofMinutes(1));
        SaSession restored = adapter.getObject(SA_TOKEN_KEY);
        assertThat(restored.getId()).isEqualTo("session-id");
        assertThat(restored.getLoginId()).isEqualTo(1L);
        assertThat(restored.getToken()).isEqualTo("token");
        assertThatThrownBy(() -> client.<UnregisteredPayload>getBucket(NAMESPACE + ":unregistered", cacheCodec)
                .set(new UnregisteredPayload("not-allowed")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void supportsSpringCacheReadEvictAndNoNullValues() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        EnabledDictItemVo item = new EnabledDictItemVo("label", "value", 1, 0);
        cache.put("entry", item);
        assertThat(cache.get("entry", EnabledDictItemVo.class)).isEqualTo(item);
        cache.put("null-entry", null);
        assertThat(cache.get("null-entry")).isNull();
        cache.evict("entry");
        assertThat(cache.get("entry")).isNull();
    }

    @Test
    void reportsRedisFailureInsteadOfReturningAnArtificialMiss() {
        Config config = new Config().setCodec(StringCodec.INSTANCE);
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:" + envInt("REDIS_TEST_BAD_PORT", 6390))
                .setTimeout(250)
                .setConnectTimeout(250)
                .setRetryAttempts(0);
        assertThatThrownBy(() -> Redisson.create(config)).isInstanceOf(Exception.class);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int envInt(String name, int fallback) {
        return Integer.parseInt(env(name, Integer.toString(fallback)));
    }

    private record UnregisteredPayload(String value) {
    }
}
