package io.github.onedream921.alphavue.modules.monitor.vo;

/**
 * Redis 键元数据和值预览
 */
public record RedisKeyMetadataVo(String key, String category, String type, Long ttlSeconds,
                                 Long sizeBytes, String value, boolean valueTruncated) {
}
