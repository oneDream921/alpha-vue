package io.github.onedream921.alphavue.modules.log.aspect;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.framework.web.ClientAddressResolver;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.log.config.AuditLogProperties;
import io.github.onedream921.alphavue.modules.log.service.AuditLogService;
import io.github.onedream921.alphavue.modules.log.service.AuditDetailSanitizer;
import io.github.onedream921.alphavue.modules.system.entity.SysUser;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 操作日志切面
 */
@Aspect
@Component
@Order
public class OperationLogAspect {

    private static final int UNEXPECTED_EXCEPTION_STACK_LIMIT = 32_000;

    private final AuditLogService auditLogService;
    private final SysUserMapper userMapper;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ClientAddressResolver clientAddressResolver;
    private final AuditLogProperties auditLogProperties;
    private final AuditDetailSanitizer auditDetailSanitizer;

    @Autowired
    public OperationLogAspect(AuditLogService auditLogService, SysUserMapper userMapper,
            HttpServletRequest request, HttpServletResponse response, ClientAddressResolver clientAddressResolver,
            AuditLogProperties auditLogProperties, AuditDetailSanitizer auditDetailSanitizer) {
        this.auditLogService = auditLogService;
        this.userMapper = userMapper;
        this.request = request;
        this.response = response;
        this.clientAddressResolver = clientAddressResolver;
        this.auditLogProperties = auditLogProperties;
        this.auditDetailSanitizer = auditDetailSanitizer;
    }

    public OperationLogAspect(AuditLogService auditLogService, SysUserMapper userMapper,
            HttpServletRequest request, HttpServletResponse response, ClientAddressResolver clientAddressResolver,
            AuditLogProperties auditLogProperties) {
        this(auditLogService, userMapper, request, response, clientAddressResolver, auditLogProperties,
                new AuditDetailSanitizer(new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    /**
     * 执行目标方法并在 finally 中提交审计日志
     */
    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startedAt = System.nanoTime();
        LoginPrincipal principal = currentPrincipal();
        boolean succeeded = false;
        int responseCode = 500;
        String exceptionStack = null;
        Integer errorCode = null;
        String requestSummary = null;
        String responseSummary = null;
        boolean detailAllowed = operationLog.saveRequest() || operationLog.saveResponse();
        if (detailAllowed && isHardDenied(request.getRequestURI(), operationLog.operation())) detailAllowed = false;
        if (detailAllowed && operationLog.saveRequest()) requestSummary = auditDetailSanitizer.request(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            succeeded = true;
            responseCode = response.getStatus();
            if (detailAllowed && operationLog.saveResponse()) responseSummary = auditDetailSanitizer.response(result);
            return result;
        } catch (BusinessException exception) {
            responseCode = exception.code();
            errorCode = exception.code();
            exceptionStack = auditLogProperties.isCaptureBusinessExceptionStack()
                    ? stackTrace(exception) : safeBusinessSummary(exception);
            throw exception;
        } catch (Throwable exception) {
            exceptionStack = stackTrace(exception);
            throw exception;
        } finally {
            auditLogService.recordOperation(
                    principal.userId(),
                    principal.username(),
                    operationLog.module(),
                    operationLog.operation(),
                    operationLog.type(),
                    request.getMethod(),
                    request.getRequestURI(),
                    responseCode,
                    succeeded,
                    clientAddressResolver.resolve(request),
                    (System.nanoTime() - startedAt) / 1_000_000,
                    (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE), errorCode, exceptionStack,
                    request.getHeader("User-Agent"), terminalExtra("clientId"), terminalExtra("deviceId"),
                    terminalExtra("deviceName"), requestSummary, responseSummary);
        }
    }

    private static boolean isHardDenied(String uri, String operation) {
        String value = (uri + " " + operation).toLowerCase(java.util.Locale.ROOT);
        return java.util.stream.Stream.of("login", "password", "captcha", "token", "logout", "upload", "file", "secret", "key")
                .anyMatch(value::contains);
    }

    private String terminalExtra(String key) {
        try {
            Object value = StpUtil.getExtra(key, null);
            if (value != null) return value.toString();
            if ("clientId".equals(key)) return StpUtil.getLoginDeviceType();
            if ("deviceId".equals(key)) return StpUtil.getLoginDeviceId();
            return null;
        }
        catch (RuntimeException ignored) { return null; }
    }

    private LoginPrincipal currentPrincipal() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return new LoginPrincipal(null, null);
        }
        Long userId = Long.valueOf(loginId.toString());
        SysUser user = userMapper.selectById(userId);
        return new LoginPrincipal(userId, user == null ? null : user.getUsername());
    }

    private record LoginPrincipal(Long userId, String username) {
    }

    private static String stackTrace(Throwable exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        String value = writer.toString()
                .replaceAll("(?i)(password|token|cookie|secret|authorization|captcha|api[-_]?key)\\s*[:=]\\s*[^,\\s]+", "$1=[redacted]");
        return value.length() <= UNEXPECTED_EXCEPTION_STACK_LIMIT
                ? value : value.substring(0, UNEXPECTED_EXCEPTION_STACK_LIMIT);
    }

    /**
     * 预期业务拒绝不是系统故障，审计只保留公共摘要，不保存调用堆栈。
     */
    private static String safeBusinessSummary(BusinessException exception) {
        String message = exception.auditSummary();
        if (message == null || message.isBlank()) {
            return "业务请求被拒绝";
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
