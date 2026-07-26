package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 部门持久化实体，对应 sys_dept 表
 */
@Getter
@Setter
@TableName("sys_dept")
public class SysDept extends SystemEntity {
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private Integer status;
}
