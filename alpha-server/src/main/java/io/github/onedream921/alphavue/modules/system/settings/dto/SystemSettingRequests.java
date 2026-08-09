package io.github.onedream921.alphavue.modules.system.settings.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public final class SystemSettingRequests {
    private SystemSettingRequests() { }
    public record Save(@NotNull Map<String, Object> values) { }
}
