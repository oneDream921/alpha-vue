package io.github.onedream921.alphavue.modules.log.vo;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;

import java.time.LocalDateTime;

/**
 * 操作日志接口响应视图
 */
public record OperationLogVo(long id, Long userId, String username, String module, String operation, String businessType,
                             String method, String requestUri, Integer responseCode, int status,
                             String ipAddress, String location, String clientId, String deviceId, String deviceName, String browser,
                             String operatingSystem, Long durationMs, String traceId, Integer errorCode, int handlingStatus,
                             Long handledBy, LocalDateTime handledAt, LocalDateTime createdAt) {
    /**
     * 从操作日志实体转换为响应视图
     */
    public static OperationLogVo from(SysOperLog log) {
        return new OperationLogVo(log.getId(), log.getUserId(), log.getUsername(), log.getModule(),
                log.getOperation(), log.getBusinessType(), log.getMethod(), log.getRequestUri(), log.getResponseCode(),
                log.getStatus(), log.getIpAddress(), log.getLocation(), log.getClientId(), log.getDeviceId(), log.getDeviceName(),
                log.getBrowser(), log.getOperatingSystem(), log.getDurationMs(), log.getTraceId(), log.getErrorCode(),
                log.getHandlingStatus(), log.getHandledBy(), log.getHandledAt(), log.getCreatedAt());
    }
}
