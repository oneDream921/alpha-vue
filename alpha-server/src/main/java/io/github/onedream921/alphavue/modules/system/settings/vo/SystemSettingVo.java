package io.github.onedream921.alphavue.modules.system.settings.vo;

import java.util.Map;

public record SystemSettingVo(String group, Map<String, Object> values, Map<String, Boolean> secretConfigured,
                              boolean restartRequired) {
}
