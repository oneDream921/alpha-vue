package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysMenu;

import java.time.LocalDateTime;

/**
 * 菜单接口响应视图
 */
public record MenuVo(Long id, Long parentId, String title, String menuType, String path, String component,
                     String permission, String icon, Integer sortOrder, Integer visible, Integer status,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 从菜单实体转换为响应视图
     */
    public static MenuVo from(SysMenu menu) {
        return new MenuVo(menu.getId(), menu.getParentId(), menu.getTitle(), menu.getMenuType(), menu.getPath(),
                menu.getComponent(), menu.getPermission(), menu.getIcon(), menu.getSortOrder(), menu.getVisible(),
                menu.getStatus(), menu.getCreatedAt(), menu.getUpdatedAt());
    }
}
