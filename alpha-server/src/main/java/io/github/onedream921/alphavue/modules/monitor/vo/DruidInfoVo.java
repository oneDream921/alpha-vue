package io.github.onedream921.alphavue.modules.monitor.vo;

/**
 * Druid 监控入口信息。
 */
public record DruidInfoVo(
        boolean enabled,
        String path) {
}
