package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
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
    public record Save(@NotBlank @Size(max = 128)
                       @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]*") String configKey,
                       @NotBlank @Size(max = 10_000) String configValue,
                       @Size(max = 500) String description) {
    }
}
