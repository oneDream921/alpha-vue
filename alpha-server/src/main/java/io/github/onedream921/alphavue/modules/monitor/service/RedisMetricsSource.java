package io.github.onedream921.alphavue.modules.monitor.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redis INFO 采样来源。实现只负责一次只读 INFO ALL 和白名单解析。
 */
public interface RedisMetricsSource {

    RedisInfoSnapshot read();
}

/**
 * 已从 Redis INFO 中提取的安全字段，不包含原始 INFO 内容。
 */
record RedisInfoSnapshot(
        String redisVersion,
        Long uptimeSeconds,
        Long usedMemoryBytes,
        Long usedMemoryRssBytes,
        Long usedMemoryPeakBytes,
        Long maxMemoryBytes,
        Long totalSystemMemoryBytes,
        Double memoryFragmentationRatio,
        Long connectedClients,
        Long blockedClients,
        Long totalConnectionsReceived,
        Long rejectedConnections,
        Long totalCommandsProcessed,
        Long instantaneousOpsPerSecond,
        Long keyspaceHits,
        Long keyspaceMisses,
        Map<String, RedisCommandSnapshot> commands) {

    RedisInfoSnapshot {
        commands = Map.copyOf(commands);
    }

    static RedisInfoSnapshot fromInfo(Map<String, String> info) {
        return new RedisInfoSnapshot(
                safeText(info, "redis_version"),
                safeLong(info, "uptime_in_seconds"),
                safeLong(info, "used_memory"),
                safeLong(info, "used_memory_rss"),
                safeLong(info, "used_memory_peak"),
                safeLong(info, "maxmemory"),
                safeLong(info, "total_system_memory"),
                safeDouble(info, "mem_fragmentation_ratio"),
                safeLong(info, "connected_clients"),
                safeLong(info, "blocked_clients"),
                safeLong(info, "total_connections_received"),
                safeLong(info, "rejected_connections"),
                safeLong(info, "total_commands_processed"),
                safeLong(info, "instantaneous_ops_per_sec"),
                safeLong(info, "keyspace_hits"),
                safeLong(info, "keyspace_misses"),
                commandSnapshots(info));
    }

    private static Map<String, RedisCommandSnapshot> commandSnapshots(Map<String, String> info) {
        Map<String, RedisCommandSnapshot> commands = new java.util.LinkedHashMap<>();
        Pattern keyPattern = Pattern.compile("cmdstat_([A-Za-z0-9_]+(?:\\|[A-Za-z0-9_]+)*)");
        for (Map.Entry<String, String> entry : info.entrySet()) {
            Matcher matcher = keyPattern.matcher(entry.getKey());
            if (!matcher.matches()) {
                continue;
            }
            RedisCommandSnapshot command = RedisCommandSnapshot.parse(entry.getValue());
            if (command != null) {
                commands.put(matcher.group(1).toLowerCase(java.util.Locale.ROOT), command);
            }
        }
        return commands;
    }

    private static String safeText(Map<String, String> info, String key) {
        String value = info.get(key);
        return value == null || value.length() > 128 ? null : value;
    }

    private static Long safeLong(Map<String, String> info, String key) {
        try {
            String value = info.get(key);
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double safeDouble(Map<String, String> info, String key) {
        try {
            String value = info.get(key);
            return value == null ? null : Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

record RedisCommandSnapshot(Long calls, Long usec, Double usecPerCall, Long rejectedCalls, Long failedCalls) {

    static RedisCommandSnapshot parse(String value) {
        if (value == null || value.length() > 512) {
            return null;
        }
        Map<String, String> fields = new java.util.HashMap<>();
        for (String item : value.split(",")) {
            String[] pair = item.split("=", 2);
            if (pair.length == 2 && pair[0].matches("calls|usec|usec_per_call|rejected_calls|failed_calls")) {
                fields.put(pair[0], pair[1]);
            }
        }
        Long calls = parseLong(fields.get("calls"));
        if (calls == null) {
            return null;
        }
        return new RedisCommandSnapshot(calls, parseLong(fields.get("usec")),
                parseDouble(fields.get("usec_per_call")), parseLong(fields.get("rejected_calls")),
                parseLong(fields.get("failed_calls")));
    }

    private static Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        try {
            return value == null ? null : Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
