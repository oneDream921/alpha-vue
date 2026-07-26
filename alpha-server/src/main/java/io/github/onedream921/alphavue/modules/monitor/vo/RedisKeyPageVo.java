package io.github.onedream921.alphavue.modules.monitor.vo;

import java.util.List;

/**
 * Redis 键游标分页结果
 */
public record RedisKeyPageVo(List<RedisKeyMetadataVo> records, String nextCursor, boolean hasMore) {
    public RedisKeyPageVo {
        records = List.copyOf(records);
    }
}
