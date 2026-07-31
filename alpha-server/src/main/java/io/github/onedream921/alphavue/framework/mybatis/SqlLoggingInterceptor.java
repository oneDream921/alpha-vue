package io.github.onedream921.alphavue.framework.mybatis;

import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 采集 MyBatis 执行摘要，避免将参数值写入日志。
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                org.apache.ibatis.session.RowBounds.class, org.apache.ibatis.session.ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                org.apache.ibatis.session.RowBounds.class, org.apache.ibatis.session.ResultHandler.class,
                org.apache.ibatis.cache.CacheKey.class, BoundSql.class})
})
public class SqlLoggingInterceptor implements Interceptor {

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:from|join|update|into)\\s+[`\"]?([a-zA-Z0-9_\\.]+)");

    private final SqlLogRecorder recorder;

    public SqlLoggingInterceptor(SqlLogRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startedAt = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            record(invocation, result, failure, startedAt);
        }
    }

    private void record(Invocation invocation, Object result, Throwable failure, long startedAt) {
        Object[] args = invocation.getArgs();
        if (args.length < 2 || !(args[0] instanceof MappedStatement mappedStatement)) {
            return;
        }
        try {
            BoundSql boundSql = args.length >= 6 && args[5] instanceof BoundSql existingBoundSql
                    ? existingBoundSql
                    : mappedStatement.getBoundSql(args[1]);
            String sql = SqlLogSanitizer.normalize(boundSql.getSql());
            if (sql.isBlank()) {
                return;
            }
            long elapsedMs = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
            recorder.record(new SqlLogCapture(
                    mappedStatement.getId(),
                    mappedStatement.getSqlCommandType().name(),
                    tableName(sql),
                    sql,
                    elapsedMs,
                    resultSize(result, failure),
                    MDC.get(TraceIdFilter.TRACE_ID_ATTRIBUTE)));
        } catch (RuntimeException ignored) {
            // SQL 日志不能影响真实业务执行。
        }
    }

    private static Integer resultSize(Object result, Throwable failure) {
        if (failure != null) {
            return null;
        }
        if (result instanceof List<?> list) {
            return list.size();
        }
        if (result instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static String tableName(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).toLowerCase(Locale.ROOT);
    }
}
