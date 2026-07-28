package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.DictRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysDictType;
import io.github.onedream921.alphavue.modules.system.entity.SysDictItem;
import io.github.onedream921.alphavue.modules.system.mapper.SysDictItemMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysDictTypeMapper;
import io.github.onedream921.alphavue.modules.system.vo.DictItemVo;
import io.github.onedream921.alphavue.modules.system.vo.DictCacheRefreshVo;
import io.github.onedream921.alphavue.modules.system.vo.DictTypeVo;
import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 数据字典业务服务
 */
@Service
public class DictService {
    private final SysDictTypeMapper typeMapper;
    private final SysDictItemMapper itemMapper;
    private final DictCacheStore dictCacheStore;

    public DictService(SysDictTypeMapper typeMapper, SysDictItemMapper itemMapper, DictCacheStore dictCacheStore) {
        this.typeMapper = typeMapper;
        this.itemMapper = itemMapper;
        this.dictCacheStore = dictCacheStore;
    }

    /**
     * 创建字典类型
     */
    @Transactional
    public DictTypeVo createType(DictRequests.TypeSave request) {
        String typeCode = request.typeCode().trim();
        if (typeMapper.selectActiveByTypeCode(typeCode) != null) {
            throw new BusinessException(400, PublicErrorMessage.DICT_TYPE_CODE_EXISTS);
        }
        SysDictType type = new SysDictType();
        type.setTypeCode(typeCode);
        type.setTypeName(request.typeName().trim());
        type.setStatus(request.status() == null ? 1 : request.status());
        type.setRemark(normalizeRemark(request.remark()));
        try {
            typeMapper.insertType(type);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(400, PublicErrorMessage.DICT_TYPE_CODE_EXISTS);
        }
        evictTypeCache(typeCode);
        return DictTypeVo.from(type);
    }

    /**
     * 分页查询字典类型
     */
    public PageResponse<DictTypeVo> pageTypes(int pageNumber, int pageSize) {
        Page<SysDictType> page = typeMapper.selectPageActive(new Page<>(pageNumber, pageSize));
        return new PageResponse<>(page.getRecords().stream().map(DictTypeVo::from).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询字典类型详情
     */
    public DictTypeVo getType(long id) {
        return DictTypeVo.from(requireType(id));
    }

    /**
     * 更新字典类型且禁止修改类型编码
     */
    @Transactional
    public DictTypeVo updateType(long id, DictRequests.TypeSave request) {
        SysDictType type = requireType(id);
        if (!type.getTypeCode().equals(request.typeCode().trim())) {
            throw new BusinessException(400, PublicErrorMessage.DICT_TYPE_CODE_IMMUTABLE);
        }
        type.setTypeName(request.typeName().trim());
        type.setStatus(request.status() == null ? 1 : request.status());
        type.setRemark(normalizeRemark(request.remark()));
        if (typeMapper.updateType(type) != 1) {
            throw invalidRequest();
        }
        evictTypeCache(type.getTypeCode());
        return DictTypeVo.from(type);
    }

    /**
     * 逻辑删除字典类型
     */
    @Transactional
    public void deleteType(long id) {
        SysDictType type = requireType(id);
        if (itemMapper.countActiveByTypeId(id) > 0) {
            throw new BusinessException(400, PublicErrorMessage.DICT_TYPE_HAS_ITEMS);
        }
        if (typeMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
        evictTypeCache(type.getTypeCode());
    }

    private SysDictType requireType(long id) {
        SysDictType type = typeMapper.selectActiveById(id);
        if (type == null) {
            throw invalidRequest();
        }
        return type;
    }

    /**
     * 分页查询指定类型的字典项
     */
    public PageResponse<DictItemVo> pageItems(long typeId, int pageNumber, int pageSize) {
        requireType(typeId);
        Page<SysDictItem> page = itemMapper.selectPageActiveByTypeId(new Page<>(pageNumber, pageSize), typeId);
        return new PageResponse<>(page.getRecords().stream().map(DictItemVo::from).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 创建字典项
     */
    @Transactional
    public DictItemVo createItem(long typeId, DictRequests.ItemSave request) {
        SysDictType type = requireType(typeId);
        String value = request.value().trim();
        assertValueUnique(typeId, value, null);
        SysDictItem item = new SysDictItem();
        item.setTypeId(typeId);
        copyItem(request, item, value);
        try {
            itemMapper.insertItem(item);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(400, PublicErrorMessage.DICT_ITEM_VALUE_EXISTS);
        }
        evictTypeCache(type.getTypeCode());
        return DictItemVo.from(item);
    }

    /**
     * 更新字典项并保持其所属类型
     */
    @Transactional
    public DictItemVo updateItem(long id, DictRequests.ItemSave request) {
        SysDictItem item = requireItem(id);
        SysDictType type = requireType(item.getTypeId());
        String value = request.value().trim();
        assertValueUnique(item.getTypeId(), value, id);
        copyItem(request, item, value);
        try {
            if (itemMapper.updateItem(item) != 1) {
                throw invalidRequest();
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(400, PublicErrorMessage.DICT_ITEM_VALUE_EXISTS);
        }
        evictTypeCache(type.getTypeCode());
        return DictItemVo.from(item);
    }

    /**
     * 逻辑删除字典项
     */
    @Transactional
    public void deleteItem(long id) {
        SysDictItem item = requireItem(id);
        SysDictType type = requireType(item.getTypeId());
        if (itemMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
        evictTypeCache(type.getTypeCode());
    }

    /**
     * 读取启用类型下的启用字典项
     */
    public List<EnabledDictItemVo> enabledItems(String typeCode) {
        String normalizedTypeCode = typeCode.trim();
        List<EnabledDictItemVo> cached = dictCacheStore.get(normalizedTypeCode);
        if (cached != null) {
            return cached;
        }
        List<EnabledDictItemVo> items = itemMapper.selectEnabledByTypeCode(normalizedTypeCode).stream()
                .map(EnabledDictItemVo::from).toList();
        dictCacheStore.put(normalizedTypeCode, items);
        return items;
    }

    /**
     * 重建全部启用字典类型的业务读取缓存。
     */
    public DictCacheRefreshVo refreshCache() {
        List<String> typeCodes = typeMapper.selectEnabledTypeCodes();
        typeCodes.forEach(typeCode -> dictCacheStore.put(typeCode, loadEnabledItems(typeCode)));
        return new DictCacheRefreshVo(typeCodes.size());
    }

    private List<EnabledDictItemVo> loadEnabledItems(String typeCode) {
        return itemMapper.selectEnabledByTypeCode(typeCode).stream().map(EnabledDictItemVo::from).toList();
    }

    private void evictTypeCache(String typeCode) {
        if (typeCode != null && !typeCode.isBlank()) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dictCacheStore.evict(typeCode);
                    }
                });
            } else {
                dictCacheStore.evict(typeCode);
            }
        }
    }

    private SysDictItem requireItem(long id) {
        SysDictItem item = itemMapper.selectActiveById(id);
        if (item == null) {
            throw invalidRequest();
        }
        return item;
    }

    private void assertValueUnique(long typeId, String value, Long selfId) {
        SysDictItem existing = itemMapper.selectActiveByTypeIdAndValue(typeId, value);
        if (existing != null && !existing.getId().equals(selfId)) {
            throw new BusinessException(400, PublicErrorMessage.DICT_ITEM_VALUE_EXISTS);
        }
    }

    private static void copyItem(DictRequests.ItemSave request, SysDictItem item, String value) {
        item.setLabel(request.label().trim());
        item.setValue(value);
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        item.setStatus(request.status() == null ? 1 : request.status());
        item.setIsDefault(request.isDefault() == null ? 0 : request.isDefault());
        item.setRemark(normalizeRemark(request.remark()));
    }

    private static String normalizeRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            return null;
        }
        return remark.trim();
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}
