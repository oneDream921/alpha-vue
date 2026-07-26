package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysDictType;

import java.time.LocalDateTime;

/**
 * 字典类型接口响应视图
 */
public record DictTypeVo(Long id, String typeCode, String typeName, Integer status, String remark,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 从字典类型实体转换为响应视图
     */
    public static DictTypeVo from(SysDictType type) {
        return new DictTypeVo(type.getId(), type.getTypeCode(), type.getTypeName(), type.getStatus(),
                type.getRemark(), type.getCreatedAt(), type.getUpdatedAt());
    }
}
