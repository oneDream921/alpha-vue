package io.github.onedream921.alphavue.modules.monitor.vo;

/**
 * Redis 键元数据，不含值内容
 */
public record RedisKeyMetadataVo(String key, String category, String type, Long ttlSeconds,
                                 Long sizeBytes, boolean valueRedacted) {
}
