package io.github.onedream921.alphavue.modules.system.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysConfigDefinition extends SystemEntity {
    private String configKey;
    private String configName;
    private String configGroup;
    private String valueType;
    private String defaultValue;
    private Integer integerMin;
    private Integer integerMax;
    private Integer stringMaxLength;
    private String stringPattern;
    private String enumValues;
    private Boolean sensitive;
    private Boolean dynamic;
    private String runtimeBinding;
    private String status;
}
