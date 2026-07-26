package io.github.onedream921.alphavue.modules.system.vo;

/**
 * 当前用户可见的菜单路由视图
 */
public record RouteVo(long id, long parentId, String title, String menuType, String path, String component,
                      String permission, String icon, int sortOrder) {
}
