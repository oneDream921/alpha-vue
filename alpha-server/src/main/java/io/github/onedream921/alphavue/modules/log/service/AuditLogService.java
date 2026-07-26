package io.github.onedream921.alphavue.modules.log.service;

import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.entity.SysLoginLog;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysLoginLogMapper;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;

    public AuditLogService(SysLoginLogMapper loginLogMapper, SysOperLogMapper operLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operLogMapper = operLogMapper;
    }

    /**
     * 异步记录登录结果
     */
    @Async("auditTaskExecutor")
    public void recordLogin(String username, Long userId, boolean succeeded, String ipAddress) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setUserId(userId);
        log.setLoginType("PASSWORD");
        log.setStatus(succeeded ? 1 : 0);
        log.setIpAddress(ipAddress);
        log.setMessage(succeeded ? "Login succeeded" : "Login rejected");
        loginLogMapper.insert(log);
    }

    /**
     * 异步记录接口操作结果和请求元数据
     */
    @Async("auditTaskExecutor")
    public void recordOperation(Long userId, String username, String module, String operation, BusinessType type,
            String method, String requestUri, int responseCode, boolean succeeded,
            String ipAddress, long durationMs, String traceId, String exceptionStack) {
        SysOperLog log = new SysOperLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setOperation(operation);
        log.setBusinessType(type.name());
        log.setMethod(method);
        log.setRequestUri(requestUri);
        log.setRequestParams("[redacted]");
        log.setResponseCode(responseCode);
        log.setStatus(succeeded ? 1 : 0);
        log.setIpAddress(ipAddress);
        log.setDurationMs(durationMs);
        log.setTraceId(traceId);
        log.setExceptionStack(exceptionStack);
        log.setHandled(0);
        log.setHandlingStatus(0);
        operLogMapper.insert(log);
    }
}
