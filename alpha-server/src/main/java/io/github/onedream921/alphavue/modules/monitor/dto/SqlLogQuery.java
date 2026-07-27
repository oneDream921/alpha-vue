package io.github.onedream921.alphavue.modules.monitor.dto;

/**
 * SQL 日志查询条件。
 */
public record SqlLogQuery(
        int limit,
        String type,
        String keyword,
        boolean slowOnly) {
}
