package io.github.onedream921.alphavue.modules.monitor.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisMaster;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.api.redisnode.RedisSingle;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonRedisMetricsSourceTests {

    @Test
    void readsInfoAllOnceAndReturnsOnlyWhitelistedFields() {
        RedissonClient client = mock(RedissonClient.class);
        RedisSingle nodes = mock(RedisSingle.class);
        RedisMaster node = mock(RedisMaster.class);
        when(client.getRedisNodes(RedisNodes.SINGLE)).thenReturn(nodes);
        when(nodes.getInstance()).thenReturn(node);
        when(node.info(RedisNode.InfoSection.ALL)).thenReturn(Map.ofEntries(
                Map.entry("redis_version", "7.4.1"),
                Map.entry("uptime_in_seconds", "42"),
                Map.entry("used_memory", "4096"),
                Map.entry("total_system_memory", "8192"),
                Map.entry("connected_clients", "3"),
                Map.entry("total_commands_processed", "100"),
                Map.entry("cmdstat_get", "calls=10,usec=20,usec_per_call=2.0,rejected_calls=0,failed_calls=0"),
                Map.entry("cmdstat_config|get", "calls=4,usec=8,usec_per_call=2.0,rejected_calls=0,failed_calls=0"),
                Map.entry("raw_secret", "must-not-escape")));

        RedisInfoSnapshot snapshot = new RedissonRedisMetricsSource(client).read();

        assertThat(snapshot.redisVersion()).isEqualTo("7.4.1");
        assertThat(snapshot.usedMemoryBytes()).isEqualTo(4096L);
        assertThat(snapshot.totalSystemMemoryBytes()).isEqualTo(8192L);
        assertThat(snapshot.commands()).containsKey("get");
        assertThat(snapshot.commands()).containsKey("config|get");
        assertThat(snapshot.commands()).doesNotContainKey("raw_secret");
        verify(node).info(RedisNode.InfoSection.ALL);
        verify(node, never()).info(RedisNode.InfoSection.SERVER);
        verify(node, never()).info(RedisNode.InfoSection.MEMORY);
        verify(node, never()).info(RedisNode.InfoSection.CLIENTS);
        verify(node, never()).info(RedisNode.InfoSection.COMMANDSTATS);
    }

    @Test
    void ignoresMalformedNumbersAndUnsafeCommandKeys() {
        RedisInfoSnapshot snapshot = RedisInfoSnapshot.fromInfo(Map.of(
                "used_memory", "not-a-number",
                "cmdstat_get", "calls=not-a-number,usec=1",
                "cmdstat_get-info", "calls=10,usec=1"));

        assertThat(snapshot.usedMemoryBytes()).isNull();
        assertThat(snapshot.commands()).isEmpty();
    }
}
