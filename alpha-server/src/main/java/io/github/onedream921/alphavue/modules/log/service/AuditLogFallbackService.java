package io.github.onedream921.alphavue.modules.log.service;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Redis 不可用时的有界异步数据库降级通道。 */
@Service
public class AuditLogFallbackService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogFallbackService.class);
    private final SysOperLogMapper operLogMapper;
    private final AuditLogMetrics metrics;

    @Autowired
    public AuditLogFallbackService(SysOperLogMapper operLogMapper, AuditLogMetrics metrics) {
        this.operLogMapper = operLogMapper;
        this.metrics = metrics;
    }

    @Async("auditTaskExecutor")
    public void persist(SysOperLog logEntry) {
        try {
            operLogMapper.insert(logEntry);
            metrics.fallback();
        } catch (RuntimeException exception) {
            log.error("操作日志降级数据库写入失败，eventId={}", logEntry.getEventId(), exception);
        }
    }
}
