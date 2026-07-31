package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.framework.mybatis.SqlLogCapture;
import io.github.onedream921.alphavue.framework.mybatis.SqlLogRecorder;
import io.github.onedream921.alphavue.framework.mybatis.SqlLogSanitizer;
import io.github.onedream921.alphavue.modules.monitor.config.SqlMonitorProperties;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogQuery;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogSettingsRequest;
import io.github.onedream921.alphavue.modules.monitor.vo.SqlLogEntryVo;
import io.github.onedream921.alphavue.modules.monitor.vo.SqlLogSettingsVo;
import io.github.onedream921.alphavue.modules.monitor.vo.SqlLogStatementVo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 最近 SQL 日志服务。
 */
@Service
public class SqlLogService implements SqlLogRecorder {

    private final ConcurrentLinkedDeque<SqlLogEntryVo> entries = new ConcurrentLinkedDeque<>();
    private final Set<String> discoveredStatementIds = new ConcurrentSkipListSet<>();
    private final Set<String> excludedStatementIds = new ConcurrentSkipListSet<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicLong sequence = new AtomicLong();
    private final SqlMonitorProperties properties;

    public SqlLogService(SqlMonitorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void record(SqlLogCapture capture) {
        discoveredStatementIds.add(capture.statementId());
        if (!enabled.get() || excludedStatementIds.contains(capture.statementId())) {
            return;
        }
        Instant createdAt = Instant.now();
        SqlLogEntryVo entry = new SqlLogEntryVo(
                sequence.incrementAndGet(),
                createdAt,
                capture.traceId(),
                capture.statementId(),
                capture.sqlCommandType(),
                capture.tableName(),
                SqlLogSanitizer.bound(capture.sql(), properties.getMaxSqlLength()),
                capture.elapsedMs(),
                capture.elapsedMs() >= properties.getSlowThresholdMs(),
                capture.resultSize());
        entries.addFirst(entry);
        trim();
    }

    public List<SqlLogEntryVo> recent(SqlLogQuery query) {
        trim();
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

    public SqlLogSettingsVo settings() {
        List<SqlLogStatementVo> statements = discoveredStatementIds.stream()
                .map(SqlLogService::statementVo)
                .toList();
        return new SqlLogSettingsVo(enabled.get(), statements, Set.copyOf(excludedStatementIds));
    }

    public SqlLogSettingsVo updateSettings(SqlLogSettingsRequest request) {
        enabled.set(request.enabled());
        excludedStatementIds.clear();
        request.excludedStatementIds().stream()
                .filter(statementId -> statementId != null && !statementId.isBlank())
                .map(String::trim)
                .forEach(excludedStatementIds::add);
        return settings();
    }

    private void trim() {
        int maxEntries = Math.max(1, properties.getMaxEntries());
        Instant expiresBefore = Instant.now().minusMillis(Math.max(0, properties.getRetentionMs()));
        entries.removeIf(entry -> entry.createdAt().isBefore(expiresBefore));
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

    private static SqlLogStatementVo statementVo(String statementId) {
        int separator = statementId.lastIndexOf('.');
        if (separator < 0) {
            return new SqlLogStatementVo(statementId, statementId, statementId);
        }
        return new SqlLogStatementVo(statementId,
                statementId.substring(0, separator),
                statementId.substring(separator + 1));
    }
}
