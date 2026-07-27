package io.github.onedream921.alphavue.framework.mybatis;

/**
 * MyBatis SQL 执行摘要，保留占位符 SQL，不记录真实参数值。
 */
public record SqlLogCapture(
        String statementId,
        String sqlCommandType,
        String tableName,
        String sql,
        long elapsedMs,
        Integer resultSize,
        String traceId) {
}
