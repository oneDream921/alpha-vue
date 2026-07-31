package io.github.onedream921.alphavue.modules.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SQL 监控配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.monitor.sql")
public class SqlMonitorProperties {

    private int maxEntries = 200;

    private long slowThresholdMs = 500;

    private long retentionMs = 1_800_000;

    private int maxSqlLength = 4_096;

}
