package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_user")
public class SysUser extends SystemEntity {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    @TableField("dept_id")
    private Long deptId;
    private Integer status;
    @TableField("must_change_password")
    private Integer mustChangePassword;
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}
