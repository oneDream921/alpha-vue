package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dept")
public class SysDept extends SystemEntity {
    @TableField("parent_id")
    private Long parentId;
    private String name;
    @TableField("sort_order")
    private Integer sortOrder;
    private Integer status;
}
