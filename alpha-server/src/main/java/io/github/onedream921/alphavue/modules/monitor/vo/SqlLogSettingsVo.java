package io.github.onedream921.alphavue.modules.monitor.vo;

import java.util.List;
import java.util.Set;

/**
 * SQL 日志采集运行时设置。
 */
public record SqlLogSettingsVo(
        boolean enabled,
        List<SqlLogStatementVo> statements,
        Set<String> excludedStatementIds) {
}
