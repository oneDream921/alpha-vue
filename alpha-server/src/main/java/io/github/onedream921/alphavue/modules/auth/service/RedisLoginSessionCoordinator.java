package io.github.onedream921.alphavue.modules.auth.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 生产环境使用 Redis 分布式锁保护会话替换窗口。
 */
@Component
@Profile("!test")
public class RedisLoginSessionCoordinator implements LoginSessionCoordinator {

    private final RedissonClient redissonClient;

    public RedisLoginSessionCoordinator(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(long userId, String clientId, Supplier<T> action) {
        RLock lock = redissonClient.getLock("alpha:auth:session:" + userId + ':' + clientId);
        lock.lock();
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
