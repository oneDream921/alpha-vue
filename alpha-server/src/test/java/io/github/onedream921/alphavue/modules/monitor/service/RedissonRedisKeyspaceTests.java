package io.github.onedream921.alphavue.modules.monitor.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisMaster;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.api.redisnode.RedisSingle;
import org.redisson.api.options.KeysScanOptions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonRedisKeyspaceTests {

    @Test
    void scansThroughBoundedKeysOptionsInsteadOfUnboundedPatternIterator() {
        RedissonClient client = mock(RedissonClient.class);
        RKeys keys = mock(RKeys.class);
        when(client.getKeys()).thenReturn(keys);
        when(keys.getKeys(any(KeysScanOptions.class))).thenReturn(List.of());

        RedisScanResult result = new RedissonRedisKeyspace(client).scan(null, "missing", "0", 100);

        assertThat(result.records()).isEmpty();
        verify(keys).getKeys(any(KeysScanOptions.class));
        verify(keys, never()).getKeysByPattern(anyString(), anyInt());
    }

    @Test
    void overviewUsesRedisKeyCountWithoutScanningTheKeyspace() {
        RedissonClient client = mock(RedissonClient.class);
        RKeys keys = mock(RKeys.class);
        RedisSingle nodes = mock(RedisSingle.class);
        RedisMaster node = mock(RedisMaster.class);
        when(client.getKeys()).thenReturn(keys);
        when(keys.count()).thenReturn(42L);
        when(client.getRedisNodes(RedisNodes.SINGLE)).thenReturn(nodes);
        when(nodes.getInstance()).thenReturn(node);
        when(node.info(org.redisson.api.redisnode.RedisNode.InfoSection.SERVER))
                .thenReturn(Map.of("redis_version", "7.4.1", "uptime_in_seconds", "12"));
        when(node.info(org.redisson.api.redisnode.RedisNode.InfoSection.CLIENTS))
                .thenReturn(Map.of("connected_clients", "3"));
        when(node.info(org.redisson.api.redisnode.RedisNode.InfoSection.MEMORY))
                .thenReturn(Map.of("used_memory", "4096"));

        RedisOverview overview = new RedissonRedisKeyspace(client).overview();

        assertThat(overview.managedKeyCounts()).containsEntry("全部 Redis 键", 42L);
        verify(keys, never()).getKeys(any(KeysScanOptions.class));
        verify(keys, never()).getKeysByPattern(anyString(), anyInt());
    }
}
