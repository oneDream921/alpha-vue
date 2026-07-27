package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.framework.mybatis.SqlLogCapture;
import io.github.onedream921.alphavue.framework.mybatis.SqlLogRecorder;
import io.github.onedream921.alphavue.modules.monitor.config.SqlMonitorProperties;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogQuery;
import io.github.onedream921.alphavue.modules.monitor.vo.DruidInfoVo;
import io.github.onedream921.alphavue.modules.monitor.vo.SqlLogEntryVo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 最近 SQL 日志服务。
 */
@Service
public class SqlLogService implements SqlLogRecorder {

    private final ConcurrentLinkedDeque<SqlLogEntryVo> entries = new ConcurrentLinkedDeque<>();
    private final AtomicLong sequence = new AtomicLong();
    private final SqlMonitorProperties properties;

    public SqlLogService(SqlMonitorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void record(SqlLogCapture capture) {
        SqlLogEntryVo entry = new SqlLogEntryVo(
                sequence.incrementAndGet(),
                Instant.now(),
                capture.traceId(),
                capture.statementId(),
                capture.sqlCommandType(),
                capture.tableName(),
                capture.sql(),
                capture.elapsedMs(),
                capture.elapsedMs() >= properties.getSlowThresholdMs(),
                capture.resultSize());
        entries.addFirst(entry);
        trim();
    }

    public List<SqlLogEntryVo> recent(SqlLogQuery query) {
        String type = normalize(query.type());
        String keyword = normalize(query.keyword());
        return entries.stream()
                .filter(entry -> type == null || entry.sqlCommandType().equalsIgnoreCase(type))
                .filter(entry -> !query.slowOnly() || entry.slow())
                .filter(entry -> keyword == null || matchesKeyword(entry, keyword))
                .limit(query.limit())
                .sorted(Comparator.comparing(SqlLogEntryVo::id).reversed())
                .toList();
    }

    public void clear() {
        entries.clear();
    }

    public DruidInfoVo druidInfo() {
        return new DruidInfoVo(properties.isDruidEnabled(), properties.getDruidPath());
    }

    private void trim() {
        int maxEntries = Math.max(1, properties.getMaxEntries());
        while (entries.size() > maxEntries) {
            entries.pollLast();
        }
    }

    private static boolean matchesKeyword(SqlLogEntryVo entry, String keyword) {
        return contains(entry.sql(), keyword)
                || contains(entry.statementId(), keyword)
                || contains(entry.tableName(), keyword)
                || contains(entry.traceId(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
