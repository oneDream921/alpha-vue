package io.github.onedream921.alphavue.modules.log.vo;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;

/** 受独立权限保护的操作日志详情。 */
public record OperationLogDetailVo(OperationLogVo summary, String exceptionStack,
                                   String requestSummary, String responseSummary) {
    public static OperationLogDetailVo from(SysOperLog log) {
        return new OperationLogDetailVo(new OperationLogVo(log.getId(), log.getUserId(), log.getUsername(),
                log.getModule(), log.getOperation(), log.getBusinessType(), log.getMethod(), log.getRequestUri(),
                log.getResponseCode(), log.getStatus(), log.getIpAddress(), log.getLocation(), log.getClientId(),
                log.getDeviceId(), log.getDeviceName(), log.getBrowser(), log.getOperatingSystem(), log.getDurationMs(),
                log.getTraceId(), log.getErrorCode(), log.getHandlingStatus(), log.getHandledBy(), log.getHandledAt(),
                log.getCreatedAt()), log.getExceptionStack(), log.getRequestSummary(), log.getResponseSummary());
    }
}
