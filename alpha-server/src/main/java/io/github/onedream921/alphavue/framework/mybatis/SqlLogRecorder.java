package io.github.onedream921.alphavue.framework.mybatis;

/**
 * SQL 日志记录入口，由监控模块提供具体存储。
 */
public interface SqlLogRecorder {

    void record(SqlLogCapture capture);
}
