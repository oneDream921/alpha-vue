package io.github.onedream921.alphavue.modules.system;

import io.github.onedream921.alphavue.modules.system.dto.DictRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysDictItem;
import io.github.onedream921.alphavue.modules.system.entity.SysDictType;
import io.github.onedream921.alphavue.modules.system.mapper.SysDictItemMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysDictTypeMapper;
import io.github.onedream921.alphavue.modules.system.service.DictCacheStore;
import io.github.onedream921.alphavue.modules.system.service.DictService;
import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictServiceCacheTests {

    @Test
    void enabledItemsUseCacheAfterFirstDatabaseLoad() {
        SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
        SysDictItemMapper itemMapper = mock(SysDictItemMapper.class);
        InMemoryDictCacheStore cacheStore = new InMemoryDictCacheStore();
        DictService service = new DictService(typeMapper, itemMapper, cacheStore);
        SysDictItem item = item("启用", "enabled");
        when(itemMapper.selectEnabledByTypeCode("dict-test.status")).thenReturn(List.of(item));

        List<EnabledDictItemVo> first = service.enabledItems(" dict-test.status ");
        List<EnabledDictItemVo> second = service.enabledItems("dict-test.status");

        assertThat(first).containsExactly(new EnabledDictItemVo("启用", "enabled", 10, 1));
        assertThat(second).containsExactlyElementsOf(first);
        verify(itemMapper, times(1)).selectEnabledByTypeCode("dict-test.status");
    }

    @Test
    void itemWritesEvictTheOwningTypeCache() {
        SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
        SysDictItemMapper itemMapper = mock(SysDictItemMapper.class);
        InMemoryDictCacheStore cacheStore = new InMemoryDictCacheStore();
        DictService service = new DictService(typeMapper, itemMapper, cacheStore);
        SysDictType type = type(7L, "dict-test.status");
        cacheStore.put("dict-test.status", List.of(new EnabledDictItemVo("旧值", "old", 0, 0)));
        when(typeMapper.selectActiveById(7L)).thenReturn(type);
        when(itemMapper.insertItem(org.mockito.ArgumentMatchers.any(SysDictItem.class))).thenReturn(1);

        service.createItem(7L, new DictRequests.ItemSave("启用", "enabled", 10, 1, 1, null));

        assertThat(cacheStore.get("dict-test.status")).isNull();
    }

    @Test
    void refreshCacheWritesEnabledTypeCachesIncludingEmptyLists() {
        SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
        SysDictItemMapper itemMapper = mock(SysDictItemMapper.class);
        InMemoryDictCacheStore cacheStore = new InMemoryDictCacheStore();
        DictService service = new DictService(typeMapper, itemMapper, cacheStore);
        when(typeMapper.selectEnabledTypeCodes()).thenReturn(List.of("dict-test.empty", "dict-test.status"));
        when(itemMapper.selectEnabledByTypeCode("dict-test.status")).thenReturn(List.of(item("启用", "enabled")));

        assertThat(service.refreshCache().typeCount()).isEqualTo(2);

        assertThat(cacheStore.get("dict-test.empty")).isEmpty();
        assertThat(cacheStore.get("dict-test.status"))
                .containsExactly(new EnabledDictItemVo("启用", "enabled", 10, 1));
    }

    private static SysDictType type(long id, String typeCode) {
        SysDictType type = new SysDictType();
        type.setId(id);
        type.setTypeCode(typeCode);
        type.setStatus(1);
        return type;
    }

    private static SysDictItem item(String label, String value) {
        SysDictItem item = new SysDictItem();
        item.setLabel(label);
        item.setValue(value);
        item.setSortOrder(10);
        item.setIsDefault(1);
        return item;
    }

    private static final class InMemoryDictCacheStore implements DictCacheStore {
        private final Map<String, List<EnabledDictItemVo>> values = new HashMap<>();

        @Override
        public List<EnabledDictItemVo> get(String typeCode) {
            return values.get(typeCode);
        }

        @Override
        public void put(String typeCode, List<EnabledDictItemVo> items) {
            values.put(typeCode, items);
        }

        @Override
        public void evict(String typeCode) {
            values.remove(typeCode);
        }
    }
}
