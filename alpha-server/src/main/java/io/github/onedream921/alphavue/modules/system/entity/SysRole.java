package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色持久化实体，对应 sys_role 表
 */
@Getter
@Setter
@TableName("sys_role")
public class SysRole extends SystemEntity {
    private String name;
    private String code;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
