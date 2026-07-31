package io.github.onedream921.alphavue.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 登录日志持久化实体，对应 sys_login_log 表
 */
@Getter
@Setter
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long userId;

    private String loginType;

    private Integer status;

    private String ipAddress;
    private String location;

    private String userAgent;
    private String clientId;
    private String deviceId;
    private String deviceName;
    private String browser;
    private String operatingSystem;
    private String traceId;
    private String errorMessage;

    private String message;

    private LocalDateTime createdAt;
}
