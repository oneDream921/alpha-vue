package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_menu")
public class SysMenu extends SystemEntity {
    @TableField("parent_id")
    private Long parentId;
    private String title;
    @TableField("menu_type")
    private String menuType;
    private String path;
    private String component;
    private String permission;
    private String icon;
    @TableField("sort_order")
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
}
