package io.github.onedream921.alphavue.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 操作日志持久化实体，对应 sys_oper_log 表
 */
@Getter
@Setter
@TableName("sys_oper_log")
public class SysOperLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;
    private String module;
    private String operation;
    private String businessType;
    private String method;
    private String requestUri;
    private String requestParams;
    private String requestSummary;
    private String responseSummary;
    private Integer responseCode;

    private Integer status;
    private String ipAddress;
    private String location;
    private String clientId;
    private String deviceId;
    private String deviceName;
    private String browser;
    private String operatingSystem;
    private Long durationMs;
    private String traceId;
    private Integer errorCode;
    private String exceptionStack;
    private Integer handled;
    private Integer handlingStatus;
    private Long handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
