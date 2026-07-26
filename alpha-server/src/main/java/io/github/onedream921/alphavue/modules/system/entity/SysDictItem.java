package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典项持久化实体，对应 sys_dict_item 表
 */
@Getter
@Setter
@TableName("sys_dict_item")
public class SysDictItem extends SystemEntity {
    private Long typeId;
    private String label;
    private String value;
    private Integer sortOrder;
    private Integer status;
    private Integer isDefault;
    private String remark;
}
