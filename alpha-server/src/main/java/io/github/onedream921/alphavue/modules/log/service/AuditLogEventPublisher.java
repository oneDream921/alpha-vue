package io.github.onedream921.alphavue.modules.log.service;

import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;

/** 将已经脱敏的操作日志事件投递到可靠的异步传输层。 */
public interface AuditLogEventPublisher {
    void publish(SysOperLog log);
}
