package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统参数持久化实体，对应 sys_config 表
 */
@Getter
@Setter
@TableName("sys_config")
public class SysConfig extends SystemEntity {
    private String configName;
    private String configKey;
    private String configValue;
    private String configGroup;
    private String dataType;
    private Boolean enabled;
    private String description;
}
