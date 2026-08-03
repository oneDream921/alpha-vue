package io.github.onedream921.alphavue.modules.log.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** 审计队列投递、消费和失败指标。 */
@Component
public class AuditLogMetrics {
    private final Counter published;
    private final Counter consumed;
    private final Counter failed;
    private final Counter fallback;
    private final Counter deadLetter;

    public AuditLogMetrics(MeterRegistry registry) {
        published = counter(registry, "alpha.audit.events.published", "Audit events published to Redis Stream");
        consumed = counter(registry, "alpha.audit.events.consumed", "Audit events persisted and acknowledged");
        failed = counter(registry, "alpha.audit.events.failed", "Audit event persistence failures");
        fallback = counter(registry, "alpha.audit.events.fallback", "Audit events using database fallback");
        deadLetter = counter(registry, "alpha.audit.events.dead_letter", "Audit events moved to dead letter stream");
    }

    private static Counter counter(MeterRegistry registry, String name, String description) {
        return Counter.builder(name).description(description).register(registry);
    }

    public void published() { published.increment(); }
    public void consumed() { consumed.increment(); }
    public void failed() { failed.increment(); }
    public void fallback() { fallback.increment(); }
    public void deadLetter() { deadLetter.increment(); }
}
