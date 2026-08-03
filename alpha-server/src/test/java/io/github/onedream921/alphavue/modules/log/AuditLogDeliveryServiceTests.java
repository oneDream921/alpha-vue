package io.github.onedream921.alphavue.modules.log;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.service.AuditLogDeliveryService;
import io.github.onedream921.alphavue.modules.log.service.AuditLogEventPublisher;
import io.github.onedream921.alphavue.modules.log.service.AuditLogFallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogDeliveryServiceTests {
    @Test
    void assignsEventIdBeforePublishing() {
        AuditLogEventPublisher publisher = mock(AuditLogEventPublisher.class);
        AuditLogFallbackService fallback = mock(AuditLogFallbackService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AuditLogEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(publisher);
        AuditLogDeliveryService service = new AuditLogDeliveryService(provider, fallback);
        SysOperLog log = new SysOperLog();

        service.deliver(log);

        assertThat(log.getEventId()).isNotBlank();
        verify(publisher).publish(log);
    }

    @Test
    void fallsBackToAsyncDatabaseWhenStreamPublishFails() {
        AuditLogEventPublisher publisher = mock(AuditLogEventPublisher.class);
        doThrow(new IllegalStateException("redis down")).when(publisher)
                .publish(org.mockito.ArgumentMatchers.any());
        AuditLogFallbackService fallback = mock(AuditLogFallbackService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AuditLogEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(publisher);
        AuditLogDeliveryService service = new AuditLogDeliveryService(provider, fallback);
        SysOperLog log = new SysOperLog();

        service.deliver(log);

        verify(fallback).persist(log);
    }
}
