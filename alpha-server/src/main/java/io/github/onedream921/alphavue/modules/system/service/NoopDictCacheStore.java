package io.github.onedream921.alphavue.modules.system.service;

import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 测试环境禁用外部 Redis 依赖。
 */
@Component
@Profile("test")
class NoopDictCacheStore implements DictCacheStore {
    @Override
    public List<EnabledDictItemVo> get(String typeCode) {
        return null;
    }

    @Override
    public void put(String typeCode, List<EnabledDictItemVo> items) {
    }

    @Override
    public void evict(String typeCode) {
    }
}
