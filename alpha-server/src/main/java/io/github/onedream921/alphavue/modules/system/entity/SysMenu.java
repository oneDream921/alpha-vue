package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 菜单和按钮权限持久化实体，对应 sys_menu 表
 */
@Getter
@Setter
@TableName("sys_menu")
public class SysMenu extends SystemEntity {
    private Long parentId;
    private String title;
    private String menuType;
    private String path;
    private String component;
    private String permission;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
}
