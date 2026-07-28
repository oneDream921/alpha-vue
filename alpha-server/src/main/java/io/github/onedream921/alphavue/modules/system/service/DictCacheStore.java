package io.github.onedream921.alphavue.modules.system.service;

import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;

import java.util.List;

/**
 * 字典业务读取缓存边界。
 */
public interface DictCacheStore {
    List<EnabledDictItemVo> get(String typeCode);

    void put(String typeCode, List<EnabledDictItemVo> items);

    void evict(String typeCode);
}
