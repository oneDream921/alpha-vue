package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 Lettuce 的 Redis 受控键访问实现
 */
@Component
@Profile("!test")
class LettuceRedisKeyspace implements RedisKeyspace {
    private static final int OVERVIEW_SCAN_COUNT = 100;
    private static final int MAX_OVERVIEW_DISCOVERED_KEYS = 10_000;

    private final RedisConnectionFactory connectionFactory;
    private final RedisManagementProperties properties;

    LettuceRedisKeyspace(RedisConnectionFactory connectionFactory, RedisManagementProperties properties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
    }

    @Override
    public RedisScanResult scan(String prefix, String cursor, int count) {
        return withCommands(commands -> {
            KeyScanCursor<byte[]> result = commands.scan(ScanCursor.of(cursor), new ScanArgs()
                    .match(prefix + "*").limit(count)).get();
            return new RedisScanResult(result.getKeys().stream().map(key -> metadata(commands, key))
                    .filter(Objects::nonNull).toList(),
                    result.getCursor());
        });
    }

    @Override
    public RedisKeyMetadata metadata(String key) {
        return withCommands(commands -> metadata(commands, key.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public boolean delete(String key) {
        return withCommands(commands -> commands.del(key.getBytes(StandardCharsets.UTF_8)).get() > 0);
    }

    @Override
    public RedisOverview overview() {
        return withCommands(commands -> {
            String serverInfo = commands.info("server").get();
            return new RedisOverview(
                    infoValue(serverInfo, "redis_version"),
                    longInfoValue(serverInfo, "uptime_in_seconds"),
                    longInfoValue(commands.info("memory").get(), "used_memory"),
                    longInfoValue(commands.info("clients").get(), "connected_clients"),
                    managedKeyCounts(commands));
        });
    }

    private RedisKeyMetadata metadata(RedisClusterAsyncCommands<byte[], byte[]> commands, byte[] rawKey) {
        try {
            String key = new String(rawKey, StandardCharsets.UTF_8);
            String type = commands.type(rawKey).get();
            if ("none".equals(type)) {
                return null;
            }
            return new RedisKeyMetadata(key, type, commands.ttl(rawKey).get(), commands.memoryUsage(rawKey).get());
        } catch (Exception exception) {
            throw new IllegalStateException("Redis 元数据查询失败", exception);
        }
    }

    private Map<String, Long> managedKeyCounts(RedisClusterAsyncCommands<byte[], byte[]> commands) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String prefix : properties.getPrefixes()) {
            long discovered = 0;
            ScanCursor cursor = ScanCursor.INITIAL;
            do {
                try {
                    KeyScanCursor<byte[]> result = commands.scan(cursor, new ScanArgs().match(prefix + "*")
                            .limit(OVERVIEW_SCAN_COUNT)).get();
                    discovered += result.getKeys().size();
                    cursor = result;
                } catch (Exception exception) {
                    throw new IllegalStateException("Redis 概览查询失败", exception);
                }
            } while (!cursor.isFinished() && discovered < MAX_OVERVIEW_DISCOVERED_KEYS);
            counts.put(prefix, discovered);
        }
        return counts;
    }

    private <T> T withCommands(CommandCallback<T> callback) {
        try (LettuceConnection connection = (LettuceConnection) connectionFactory.getConnection()) {
            return callback.apply(connection.getNativeConnection());
        } catch (Exception exception) {
            throw new IllegalStateException("Redis 运维访问失败", exception);
        }
    }

    private static String infoValue(String info, String name) {
        for (String line : info.split("\\r?\\n")) {
            if (line.startsWith(name + ':')) {
                return line.substring(name.length() + 1);
            }
        }
        return null;
    }

    private static Long longInfoValue(String info, String name) {
        String value = infoValue(info, name);
        return value == null ? null : Long.parseLong(value);
    }

    @FunctionalInterface
    private interface CommandCallback<T> {
        T apply(RedisClusterAsyncCommands<byte[], byte[]> commands) throws Exception;
    }
}
