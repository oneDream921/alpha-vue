package io.github.onedream921.alphavue.framework.redis;

import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 对象迁移边界。
 *
 * <p>只服务于新的 alpha 验证键，不接入现有会话 DAO，也不读取旧 JDK 序列化数据。</p>
 */
@Component
@Profile("!test")
public class RedissonSaTokenObjectAdapter {

    private final RedissonClient client;
    private final Codec codec;

    public RedissonSaTokenObjectAdapter(RedissonClient client,
                                         @Qualifier("redissonSaTokenCodec") Codec codec) {
        this.client = client;
        this.codec = codec;
    }

    public <T> void setObject(RedisKey key, T value, Duration ttl) {
        client.<T>getBucket(key.value(), codec).set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    public <T> T getObject(RedisKey key) {
        return client.<T>getBucket(key.value(), codec).get();
    }

    public boolean deleteObject(RedisKey key) {
        return client.getBucket(key.value(), codec).delete();
    }
}
