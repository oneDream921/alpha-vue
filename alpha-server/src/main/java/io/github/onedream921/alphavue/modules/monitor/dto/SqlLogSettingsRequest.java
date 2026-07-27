package io.github.onedream921.alphavue.modules.monitor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * SQL 日志采集设置。
 */
public record SqlLogSettingsRequest(
        @NotNull Boolean enabled,
        @NotNull @Size(max = 500) Set<@Size(max = 256) String> excludedStatementIds) {
}
