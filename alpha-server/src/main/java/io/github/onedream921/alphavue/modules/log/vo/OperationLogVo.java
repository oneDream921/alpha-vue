package io.github.onedream921.alphavue.modules.log.vo;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;

import java.time.LocalDateTime;

/**
 * 操作日志接口响应视图
 */
public record OperationLogVo(long id, Long userId, String username, String module, String operation, String businessType,
                             String method, String requestUri, Integer responseCode, int status,
                             String ipAddress, Long durationMs, String traceId, int handlingStatus,
                             Long handledBy, LocalDateTime handledAt, LocalDateTime createdAt) {
    /**
     * 从操作日志实体转换为响应视图
     */
    public static OperationLogVo from(SysOperLog log) {
        return new OperationLogVo(log.getId(), log.getUserId(), log.getUsername(), log.getModule(),
                log.getOperation(), log.getBusinessType(), log.getMethod(), log.getRequestUri(), log.getResponseCode(),
                log.getStatus(), log.getIpAddress(), log.getDurationMs(), log.getTraceId(),
                log.getHandlingStatus(), log.getHandledBy(), log.getHandledAt(), log.getCreatedAt());
    }
}
