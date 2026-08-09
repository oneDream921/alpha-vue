package io.github.onedream921.alphavue.modules.system.settings.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.onedream921.alphavue.modules.system.entity.SystemEntity;
import lombok.Getter;
import lombok.Setter;

/** A registered system-settings group. Sensitive values are stored separately. */
@Getter
@Setter
@TableName("sys_system_setting")
public class SysSystemSetting extends SystemEntity {
    private String settingGroup;
    private String valuesJson;
    private String secretsCiphertext;
    private Integer keyVersion;
}
