package io.github.onedream921.alphavue.modules.log;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.log.aspect.OperationLogAspect;
import io.github.onedream921.alphavue.modules.log.service.AuditLogService;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTests {

    @Test
    void recordsBusinessExceptionSummaryWithoutStack() throws Throwable {
        AuditLogService auditLogService = mock(AuditLogService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        OperationLog operationLog = mock(OperationLog.class);
        BusinessException exception = new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
        when(operationLog.module()).thenReturn("Test");
        when(operationLog.operation()).thenReturn("Reject request");
        when(operationLog.type()).thenReturn(BusinessType.UPDATE);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(joinPoint.proceed()).thenThrow(exception);
        OperationLogAspect aspect = new OperationLogAspect(auditLogService, mock(SysUserMapper.class), request,
                mock(HttpServletResponse.class), mock(io.github.onedream921.alphavue.framework.web.ClientAddressResolver.class));

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);

            assertThatThrownBy(() -> aspect.record(joinPoint, operationLog)).isSameAs(exception);

            ArgumentCaptor<String> exceptionStack = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).recordOperation(isNull(), isNull(), eq("Test"), eq("Reject request"),
                    eq(BusinessType.UPDATE), eq("PUT"), eq("/api/test"), eq(400), eq(false), isNull(), anyLong(),
                    isNull(), eq(400), exceptionStack.capture(), isNull(), isNull(), isNull(), isNull());
            assertThat(exceptionStack.getValue()).isEqualTo("请求参数错误");
            assertThat(exceptionStack.getValue()).doesNotContain("\n\tat ");
        }
    }

    @Test
    void recordsInternalBusinessSummaryWithoutExposingItToThePublicMessage() throws Throwable {
        AuditLogService auditLogService = mock(AuditLogService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        OperationLog operationLog = mock(OperationLog.class);
        BusinessException exception = new BusinessException(400, PublicErrorMessage.INVALID_REQUEST, "图片内容签名校验失败");
        when(operationLog.module()).thenReturn("Test");
        when(operationLog.operation()).thenReturn("Reject image");
        when(operationLog.type()).thenReturn(BusinessType.UPDATE);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test/image");
        when(joinPoint.proceed()).thenThrow(exception);
        OperationLogAspect aspect = new OperationLogAspect(auditLogService, mock(SysUserMapper.class), request,
                mock(HttpServletResponse.class), mock(io.github.onedream921.alphavue.framework.web.ClientAddressResolver.class));

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);

            assertThat(exception.getMessage()).isEqualTo("请求参数错误");
            assertThatThrownBy(() -> aspect.record(joinPoint, operationLog)).isSameAs(exception);

            ArgumentCaptor<String> exceptionSummary = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).recordOperation(isNull(), isNull(), eq("Test"), eq("Reject image"),
                    eq(BusinessType.UPDATE), eq("POST"), eq("/api/test/image"), eq(400), eq(false), isNull(), anyLong(),
                    isNull(), eq(400), exceptionSummary.capture(), isNull(), isNull(), isNull(), isNull());
            assertThat(exceptionSummary.getValue()).isEqualTo("图片内容签名校验失败");
        }
    }

    @Test
    void recordsUnexpectedExceptionStackForDiagnosis() throws Throwable {
        AuditLogService auditLogService = mock(AuditLogService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        OperationLog operationLog = mock(OperationLog.class);
        IllegalStateException exception = new IllegalStateException("unexpected failure");
        when(operationLog.module()).thenReturn("Test");
        when(operationLog.operation()).thenReturn("Unexpected failure");
        when(operationLog.type()).thenReturn(BusinessType.UPDATE);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(joinPoint.proceed()).thenThrow(exception);
        OperationLogAspect aspect = new OperationLogAspect(auditLogService, mock(SysUserMapper.class), request,
                mock(HttpServletResponse.class), mock(io.github.onedream921.alphavue.framework.web.ClientAddressResolver.class));

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);

            assertThatThrownBy(() -> aspect.record(joinPoint, operationLog)).isSameAs(exception);

            ArgumentCaptor<String> exceptionStack = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).recordOperation(isNull(), isNull(), eq("Test"), eq("Unexpected failure"),
                    eq(BusinessType.UPDATE), eq("PUT"), eq("/api/test"), eq(500), eq(false), isNull(), anyLong(),
                    isNull(), isNull(), exceptionStack.capture(), isNull(), isNull(), isNull(), isNull());
            assertThat(exceptionStack.getValue()).contains("IllegalStateException: unexpected failure");
        }
    }
}
