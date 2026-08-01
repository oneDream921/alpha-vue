package io.github.onedream921.alphavue.modules.monitor.service;

import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redisson Redis INFO 采样适配器。
 */
@Component
@Profile("!test")
class RedissonRedisMetricsSource implements RedisMetricsSource {
    private final RedissonClient client;

    RedissonRedisMetricsSource(RedissonClient client) {
        this.client = client;
    }

    @Override
    public RedisInfoSnapshot read() {
        RedisNode node = client.getRedisNodes(RedisNodes.SINGLE).getInstance();
        Map<String, String> info = node.info(RedisNode.InfoSection.ALL);
        return RedisInfoSnapshot.fromInfo(info);
    }
}
