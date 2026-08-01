package io.github.onedream921.alphavue.modules.monitor.service;

import org.redisson.api.RKeys;
import org.redisson.api.RType;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.api.redisnode.RedisSingle;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.ByteArrayCodec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Redisson-backed Redis management access with bounded SCAN and no object decoding. */
@Component
@Profile("!test")
class RedissonRedisKeyspace implements RedisKeyspace {
    private static final int MAX_PAGE_SCAN = 10_000;
    private static final int VALUE_CHAR_LIMIT = 16_384;
    private final RedissonClient client;

    RedissonRedisKeyspace(RedissonClient client) {
        this.client = client;
    }

    @Override
    public RedisScanResult scan(String prefix, String keyword, String cursor, int count) {
        int offset = parseCursor(cursor);
        int safeCount = Math.min(Math.max(count, 1), 100);
        int limit = Math.min(MAX_PAGE_SCAN, offset + safeCount);
        List<RedisKeyMetadata> matching = new ArrayList<>();
        KeysScanOptions options = KeysScanOptions.defaults()
                .pattern(pattern(prefix))
                .limit(MAX_PAGE_SCAN)
                .chunkSize(Math.min(limit, 100));
        for (String key : client.getKeys().getKeys(options)) {
            if (keyword == null || key.toLowerCase(Locale.ROOT).contains(keyword)) {
                RedisKeyMetadata metadata = metadata(key);
                if (metadata != null) matching.add(metadata);
            }
            if (matching.size() >= limit) break;
        }
        int from = Math.min(offset, matching.size());
        int to = Math.min(from + safeCount, matching.size());
        String next = to < matching.size() ? Integer.toString(to) : "0";
        return new RedisScanResult(matching.subList(from, to), next);
    }

    @Override
    public RedisKeyMetadata metadata(String key) {
        try {
            RType type = client.getKeys().getType(key);
            if (type == null) return null;
            String typeName = type.getValue();
            var object = client.getBucket(key);
            Long size = object.sizeInMemory() >= 0 ? object.sizeInMemory() : null;
            Long ttl = object.remainTimeToLive();
            String value = preview(key, typeName);
            return new RedisKeyMetadata(key, typeName, ttl < 0 ? ttl : ttl / 1000, size, value,
                    value != null && value.length() >= VALUE_CHAR_LIMIT);
        } catch (Exception exception) {
            throw new IllegalStateException("Redis 元数据查询失败", exception);
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            return client.getKeys().delete(key) > 0;
        } catch (Exception exception) {
            throw new IllegalStateException("Redis 运维删除失败", exception);
        }
    }

    @Override
    public RedisOverview overview() {
        RedisSingle nodes = client.getRedisNodes(RedisNodes.SINGLE);
        RedisNode node = nodes.getInstance();
        Map<String, String> server = node.info(RedisNode.InfoSection.SERVER);
        Map<String, String> clients = node.info(RedisNode.InfoSection.CLIENTS);
        Map<String, String> memory = node.info(RedisNode.InfoSection.MEMORY);
        long discovered = client.getKeys().count();
        return new RedisOverview(server.get("redis_version"), number(server, "uptime_in_seconds"),
                number(memory, "used_memory"), number(clients, "connected_clients"),
                new LinkedHashMap<>(Map.of("全部 Redis 键", discovered)));
    }

    private String preview(String key, String type) {
        if (!"string".equals(type)) return "[masked]";
        try {
            byte[] raw = client.<byte[]>getBucket(key, ByteArrayCodec.INSTANCE).get();
            if (raw == null) return null;
            String text = new String(raw, StandardCharsets.UTF_8);
            return text.length() > VALUE_CHAR_LIMIT ? text.substring(0, VALUE_CHAR_LIMIT) : text;
        } catch (Exception exception) {
            return "[unavailable]";
        }
    }

    private static String pattern(String prefix) {
        return prefix == null || prefix.isBlank() ? "*" : prefix + "*";
    }

    private static int parseCursor(String cursor) {
        try { return Math.max(Integer.parseInt(cursor == null ? "0" : cursor), 0); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Redis 游标无效"); }
    }

    private static Long number(Map<String, String> values, String name) {
        try { return values.containsKey(name) ? Long.parseLong(values.get(name)) : null; }
        catch (NumberFormatException exception) { return null; }
    }

}
