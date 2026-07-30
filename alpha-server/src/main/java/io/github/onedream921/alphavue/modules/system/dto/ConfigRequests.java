package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 参数配置接口的请求对象集合
 */
public final class ConfigRequests {
    private ConfigRequests() {
    }

    /**
     * 新增或更新参数配置请求
     */
    public record Save(@jakarta.validation.constraints.NotBlank @Size(max = 128)
                       @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]*") String configKey,
                       @jakarta.validation.constraints.NotBlank @Size(max = 10_000) String configValue,
                       @NotNull Boolean enabled,
                       @Size(max = 500) String ignoredDescription) {
    }

    public record DefinitionSave(@jakarta.validation.constraints.NotBlank @Size(max = 128)
                                 @Pattern(regexp = "file\\.[A-Za-z][A-Za-z0-9._-]*") String configKey,
                                 @jakarta.validation.constraints.NotBlank @Size(max = 64) String configName,
                                 @jakarta.validation.constraints.NotBlank @Pattern(regexp = "BOOLEAN|INTEGER|ENUM|STRING") String valueType,
                                 @Size(max = 10_000) String defaultValue,
                                 Integer integerMin, Integer integerMax, Integer stringMaxLength,
                                 @Size(max = 500) String stringPattern, @Size(max = 2000) String enumValues,
                                 @NotNull Boolean sensitive, @NotNull Boolean dynamic,
                                 @Size(max = 64) String runtimeBinding, @NotNull @Pattern(regexp = "DRAFT|PUBLISHED|DISABLED") String status) { }
}
