package io.github.onedream921.alphavue.modules.log.service;

import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.entity.SysLoginLog;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysLoginLogMapper;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import io.github.onedream921.alphavue.framework.web.IpLocationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    private static final int EXCEPTION_SUMMARY_LIMIT = 32_000;

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;
    private final IpLocationService ipLocationService;

    public AuditLogService(SysLoginLogMapper loginLogMapper, SysOperLogMapper operLogMapper,
            IpLocationService ipLocationService) {
        this.loginLogMapper = loginLogMapper;
        this.operLogMapper = operLogMapper;
        this.ipLocationService = ipLocationService;
    }

    /**
     * 异步记录登录结果
     */
    @Async("auditTaskExecutor")
    public void recordLogin(String username, Long userId, boolean succeeded, String ipAddress, String userAgent,
            String clientId, String deviceId, String deviceName, String traceId, Integer errorCode,
            String errorMessage) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setUserId(userId);
        log.setLoginType("PASSWORD");
        log.setStatus(succeeded ? 1 : 0);
        log.setIpAddress(ipAddress);
        log.setLocation(ipLocationService.resolve(ipAddress));
        log.setUserAgent(limit(userAgent, 1000));
        log.setClientId(limit(clientId, 64));
        log.setDeviceId(limit(deviceId, 128));
        log.setDeviceName(limit(deviceName, 128));
        log.setBrowser(browser(userAgent));
        log.setOperatingSystem(operatingSystem(userAgent));
        log.setTraceId(limit(traceId, 64));
        log.setErrorMessage(limit(errorMessage, 500));
        log.setMessage(succeeded ? "Login succeeded" : "Login rejected");
        try { loginLogMapper.insert(log); } catch (RuntimeException ignored) { }
    }

    /**
     * 异步记录接口操作结果和请求元数据
     */
    @Async("auditTaskExecutor")
    public void recordOperation(Long userId, String username, String module, String operation, BusinessType type,
            String method, String requestUri, int responseCode, boolean succeeded,
            String ipAddress, long durationMs, String traceId, Integer errorCode, String exceptionStack,
            String userAgent, String clientId, String deviceId, String deviceName,
            String requestSummary, String responseSummary) {
        SysOperLog log = new SysOperLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setOperation(operation);
        log.setBusinessType(type.name());
        log.setMethod(method);
        log.setRequestUri(requestUri);
        log.setRequestParams("[redacted]");
        log.setRequestSummary(limit(requestSummary, 16_000));
        log.setResponseSummary(limit(responseSummary, 2_000));
        log.setResponseCode(responseCode);
        log.setStatus(succeeded ? 1 : 0);
        log.setIpAddress(ipAddress);
        log.setLocation(ipLocationService.resolve(ipAddress));
        log.setClientId(limit(clientId, 64));
        log.setDeviceId(limit(deviceId, 128));
        log.setDeviceName(limit(deviceName, 128));
        log.setBrowser(browser(userAgent));
        log.setOperatingSystem(operatingSystem(userAgent));
        log.setDurationMs(durationMs);
        log.setTraceId(traceId);
        log.setErrorCode(errorCode);
        log.setExceptionStack(limit(exceptionStack, EXCEPTION_SUMMARY_LIMIT));
        log.setHandled(0);
        log.setHandlingStatus(0);
        try { operLogMapper.insert(log); } catch (RuntimeException ignored) { }
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String browser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/")) return "Safari";
        return "Unknown";
    }

    private static String operatingSystem(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Linux")) return "Linux";
        return "Unknown";
    }
}
