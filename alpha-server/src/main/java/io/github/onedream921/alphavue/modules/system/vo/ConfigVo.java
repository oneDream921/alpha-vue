package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysConfig;

import java.time.LocalDateTime;

/**
 * 参数配置接口响应视图
 */
public record ConfigVo(Long id, String configName, String configKey, String configValue, String configGroup,
                       String dataType, Boolean enabled, String description,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 从参数配置实体转换为响应视图
     */
    public static ConfigVo from(SysConfig config) {
        return new ConfigVo(config.getId(), config.getConfigName(), config.getConfigKey(), config.getConfigValue(),
                config.getConfigGroup(), config.getDataType(), config.getEnabled(), config.getDescription(),
                config.getCreatedAt(), config.getUpdatedAt());
    }
}
