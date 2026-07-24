package io.github.onedream921.alphavue.modules.log;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Captures operation metadata only; request and response payloads stay redacted. */
@Aspect
@Component
@Order
public class OperationLogAspect {

    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public OperationLogAspect(AuditLogService auditLogService, JdbcTemplate jdbcTemplate,
            HttpServletRequest request, HttpServletResponse response) {
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
        this.request = request;
        this.response = response;
    }

    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startedAt = System.nanoTime();
        LoginPrincipal principal = currentPrincipal();
        boolean succeeded = false;
        int responseCode = 500;
        try {
            Object result = joinPoint.proceed();
            succeeded = true;
            responseCode = response.getStatus();
            return result;
        } catch (BusinessException exception) {
            responseCode = exception.code();
            throw exception;
        } finally {
            auditLogService.recordOperation(
                    principal.userId(),
                    principal.username(),
                    operationLog.module(),
                    operationLog.operation(),
                    request.getMethod(),
                    request.getRequestURI(),
                    responseCode,
                    succeeded,
                    request.getRemoteAddr(),
                    (System.nanoTime() - startedAt) / 1_000_000,
                    (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE));
        }
    }

    private LoginPrincipal currentPrincipal() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return new LoginPrincipal(null, null);
        }
        Long userId = Long.valueOf(loginId.toString());
        List<String> usernames = jdbcTemplate.query(
                "SELECT username FROM sys_user WHERE id = ?",
                (resultSet, rowNum) -> resultSet.getString("username"),
                userId);
        return new LoginPrincipal(userId, usernames.isEmpty() ? null : usernames.getFirst());
    }

    private record LoginPrincipal(Long userId, String username) {
    }
}
