package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysConfig;

import java.time.LocalDateTime;

/**
 * 参数配置接口响应视图
 */
public record ConfigVo(Long id, String configName, String configKey, String configValue, String configGroup,
                       String dataType, Boolean enabled, String description, String domain, Boolean sensitive,
                       Boolean dynamic, java.util.List<String> enumValues,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 从参数配置实体转换为响应视图
     */
    public static ConfigVo from(SysConfig config, io.github.onedream921.alphavue.modules.system.entity.SysConfigDefinition definition) {
        return new ConfigVo(config.getId(), definition.getConfigName(), config.getConfigKey(),
                Boolean.TRUE.equals(definition.getSensitive()) ? null : config.getConfigValue(), definition.getConfigGroup(), definition.getValueType(),
                config.getEnabled(), null, definition.getConfigGroup(), definition.getSensitive(), definition.getDynamic(),
                definition.getEnumValues() == null ? java.util.List.of() : java.util.Arrays.asList(definition.getEnumValues().split(",")),
                config.getCreatedAt(), config.getUpdatedAt());
    }
}
