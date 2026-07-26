package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysDept;

import java.time.LocalDateTime;

/**
 * 部门接口响应视图
 */
public record DeptVo(Long id, Long parentId, String name, Integer sortOrder, Integer status,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 从部门实体转换为响应视图
     */
    public static DeptVo from(SysDept dept) {
        return new DeptVo(dept.getId(), dept.getParentId(), dept.getName(), dept.getSortOrder(), dept.getStatus(),
                dept.getCreatedAt(), dept.getUpdatedAt());
    }
}
