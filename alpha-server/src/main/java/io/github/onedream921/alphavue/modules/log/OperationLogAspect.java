package io.github.onedream921.alphavue.modules.log;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Captures operation metadata only; request and response payloads stay redacted. */
@Aspect
@Component
@Order
public class OperationLogAspect {

    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public OperationLogAspect(AuditLogService auditLogService, HttpServletRequest request, HttpServletResponse response) {
        this.auditLogService = auditLogService;
        this.request = request;
        this.response = response;
    }

    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startedAt = System.nanoTime();
        boolean succeeded = false;
        try {
            Object result = joinPoint.proceed();
            succeeded = true;
            return result;
        } finally {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            Long userId = loginId == null ? null : Long.valueOf(loginId.toString());
            auditLogService.recordOperation(
                    userId,
                    null,
                    operationLog.module(),
                    operationLog.operation(),
                    request.getMethod(),
                    request.getRequestURI(),
                    succeeded ? response.getStatus() : 500,
                    succeeded,
                    request.getRemoteAddr(),
                    (System.nanoTime() - startedAt) / 1_000_000,
                    (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE));
        }
    }
}
