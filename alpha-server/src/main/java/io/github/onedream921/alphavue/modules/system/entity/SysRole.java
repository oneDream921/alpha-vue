package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_role")
public class SysRole extends SystemEntity {
    private String name;
    private String code;
    @TableField("sort_order")
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
