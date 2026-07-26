package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysDictItem;

/**
 * 已启用字典项的业务读取响应视图
 */
public record EnabledDictItemVo(String label, String value, Integer sortOrder, Integer isDefault) {
    public static EnabledDictItemVo from(SysDictItem item) {
        return new EnabledDictItemVo(item.getLabel(), item.getValue(), item.getSortOrder(), item.getIsDefault());
    }
}
