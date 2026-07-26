package io.github.onedream921.alphavue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户持久化实体，对应 sys_user 表
 */
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
    private Long deptId;
    private Integer status;
    private Integer mustChangePassword;
    private LocalDateTime lastLoginAt;
}
