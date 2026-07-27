package io.github.onedream921.alphavue.modules.monitor.vo;

/**
 * 已发现的 MyBatis statement。
 */
public record SqlLogStatementVo(
        String statementId,
        String mapperName,
        String methodName) {
}
