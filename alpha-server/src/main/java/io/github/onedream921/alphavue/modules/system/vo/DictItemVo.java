package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysDictItem;

import java.time.LocalDateTime;

/**
 * 字典项管理接口响应视图
 */
public record DictItemVo(Long id, Long typeId, String label, String value, Integer sortOrder, Integer status,
                         Integer isDefault, String remark, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static DictItemVo from(SysDictItem item) {
        return new DictItemVo(item.getId(), item.getTypeId(), item.getLabel(), item.getValue(), item.getSortOrder(),
                item.getStatus(), item.getIsDefault(), item.getRemark(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
