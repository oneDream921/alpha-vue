package io.github.onedream921.alphavue.modules.monitor.dto;

/**
 * Redis 键游标查询参数
 */
public record RedisKeyQuery(String prefix, String cursor, int count, String keyword) {
}
