package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysRole;

import java.time.LocalDateTime;

/**
 * 角色接口响应视图
 */
public record RoleVo(Long id, String name, String code, Integer sortOrder, Integer status, String remark,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 从角色实体转换为响应视图
     */
    public static RoleVo from(SysRole role) {
        return new RoleVo(role.getId(), role.getName(), role.getCode(), role.getSortOrder(), role.getStatus(),
                role.getRemark(), role.getCreatedAt(), role.getUpdatedAt());
    }
}
