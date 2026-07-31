package io.github.onedream921.alphavue.modules.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 测试环境的进程内等价实现，不引入 Redis 测试依赖。
 */
@Component
@Profile("test")
public class InMemoryLoginSessionCoordinator implements LoginSessionCoordinator {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T execute(long userId, String clientId, Supplier<T> action) {
        Object lock = locks.computeIfAbsent(userId + ':' + clientId, ignored -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }
}
