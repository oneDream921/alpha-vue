package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典类型持久化实体，对应 sys_dict_type 表
 */
@Getter
@Setter
@TableName("sys_dict_type")
public class SysDictType extends SystemEntity {
    private String typeCode;
    private String typeName;
    private Integer status;
    private String remark;
}
