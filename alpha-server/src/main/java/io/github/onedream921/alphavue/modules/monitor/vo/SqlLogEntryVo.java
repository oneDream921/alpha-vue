package io.github.onedream921.alphavue.modules.monitor.vo;

import java.time.Instant;

/**
 * 最近 SQL 执行摘要。
 */
public record SqlLogEntryVo(
        long id,
        Instant createdAt,
        String traceId,
        String statementId,
        String sqlCommandType,
        String tableName,
        String sql,
        long elapsedMs,
        boolean slow,
        Integer resultSize) {
}
