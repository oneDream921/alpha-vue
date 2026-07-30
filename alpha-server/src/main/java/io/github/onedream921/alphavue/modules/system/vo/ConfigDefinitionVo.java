package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysConfigDefinition;
import java.time.LocalDateTime;
import java.util.List;

public record ConfigDefinitionVo(Long id, String configKey, String configName, String configGroup, String valueType,
                                 String defaultValue, Integer integerMin, Integer integerMax, Integer stringMaxLength,
                                 String stringPattern, List<String> enumValues, Boolean sensitive, Boolean dynamic,
                                 String runtimeBinding, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ConfigDefinitionVo from(SysConfigDefinition value) {
        return new ConfigDefinitionVo(value.getId(), value.getConfigKey(), value.getConfigName(), value.getConfigGroup(),
                value.getValueType(), Boolean.TRUE.equals(value.getSensitive()) ? null : value.getDefaultValue(),
                value.getIntegerMin(), value.getIntegerMax(), value.getStringMaxLength(), value.getStringPattern(),
                value.getEnumValues() == null ? List.of() : List.of(value.getEnumValues().split(",")), value.getSensitive(),
                value.getDynamic(), value.getRuntimeBinding(), value.getStatus(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
