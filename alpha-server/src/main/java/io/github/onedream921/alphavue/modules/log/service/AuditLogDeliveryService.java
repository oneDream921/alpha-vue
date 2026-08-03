package io.github.onedream921.alphavue.modules.log.service;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** 统一处理操作日志的持久化交付与 Redis 故障降级。 */
@Service
public class AuditLogDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogDeliveryService.class);

    private final ObjectProvider<AuditLogEventPublisher> publisherProvider;
    private final AuditLogFallbackService fallbackService;

    public AuditLogDeliveryService(ObjectProvider<AuditLogEventPublisher> publisherProvider,
            AuditLogFallbackService fallbackService) {
        this.publisherProvider = publisherProvider;
        this.fallbackService = fallbackService;
    }

    public void deliver(SysOperLog logEntry) {
        if (logEntry.getEventId() == null || logEntry.getEventId().isBlank()) {
            logEntry.setEventId(UUID.randomUUID().toString());
        }
        AuditLogEventPublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null) {
            fallbackService.persist(logEntry);
            return;
        }
        try {
            publisher.publish(logEntry);
        } catch (RuntimeException exception) {
            log.warn("Redis Streams 审计日志投递失败，降级为异步数据库写入，eventId={}",
                    logEntry.getEventId(), exception);
            fallbackService.persist(logEntry);
        }
    }
}
