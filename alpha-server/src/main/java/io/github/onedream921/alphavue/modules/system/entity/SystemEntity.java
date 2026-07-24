package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Shared persisted lifecycle fields used by the existing sys_* tables. */
@Getter
@Setter
public abstract class SystemEntity {

    @TableId
    private Long id;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
