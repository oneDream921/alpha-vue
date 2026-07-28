package io.github.onedream921.alphavue.modules.system.service;

/**
 * 参数业务读取缓存边界。
 */
public interface ConfigCacheStore {
    String get(String configKey);

    void put(String configKey, String configValue);

    void evict(String configKey);
}
