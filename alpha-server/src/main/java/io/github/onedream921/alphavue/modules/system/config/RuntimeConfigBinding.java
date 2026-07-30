package io.github.onedream921.alphavue.modules.system.config;

/** 已实现的运行时业务配置绑定。 */
public enum RuntimeConfigBinding {
    FILE_UPLOAD_MAX_SIZE("file.upload.max-size-mb", ConfigValueType.INTEGER),
    FILE_UPLOAD_ALLOWED_EXTENSIONS("file.upload.allowed-extensions", ConfigValueType.STRING),
    FILE_PRIVATE_ACCESS_TTL("file.private-access-ttl-minutes", ConfigValueType.INTEGER);

    private final String configKey;
    private final ConfigValueType valueType;

    RuntimeConfigBinding(String configKey, ConfigValueType valueType) {
        this.configKey = configKey;
        this.valueType = valueType;
    }

    public String configKey() {
        return configKey;
    }

    public ConfigValueType valueType() {
        return valueType;
    }
}
