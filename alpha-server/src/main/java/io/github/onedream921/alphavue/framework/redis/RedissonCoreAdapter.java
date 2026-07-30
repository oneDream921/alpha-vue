package io.github.onedream921.alphavue.framework.redis;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * 核心 Redis 适配层。
 *
 * <p>适配层只接收受控 RedisKey，不承载验证码、会话、字典或配置等业务语义。</p>
 */
@Component
@Profile("!test")
public class RedissonCoreAdapter {

    private final RedissonClient client;
    private final Codec objectCodec;

    public RedissonCoreAdapter(RedissonClient client, @org.springframework.beans.factory.annotation.Qualifier("redissonCacheCodec") Codec objectCodec) {
        this.client = client;
        this.objectCodec = objectCodec;
    }

    public void putString(RedisKey key, String value, Duration ttl) {
        client.getBucket(key.value()).set(value, ttl.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public String getString(RedisKey key) {
        return client.<String>getBucket(key.value()).get();
    }

    public <T> void putObject(RedisKey key, T value, Duration ttl) {
        client.<T>getBucket(key.value(), objectCodec).set(value, ttl.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public <T> T getObject(RedisKey key) {
        return client.<T>getBucket(key.value(), objectCodec).get();
    }

    public long increment(RedisKey key) {
        RAtomicLong counter = client.getAtomicLong(key.value());
        return counter.incrementAndGet();
    }

    public boolean delete(RedisKey key) {
        return client.getBucket(key.value()).delete();
    }
}
