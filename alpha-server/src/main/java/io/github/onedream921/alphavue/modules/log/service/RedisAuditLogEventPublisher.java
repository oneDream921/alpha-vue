package io.github.onedream921.alphavue.modules.log.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.onedream921.alphavue.modules.log.config.AuditStreamProperties;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Redis Streams 操作日志 Producer。 */
@Component
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnProperty(prefix = "alpha.log.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAuditLogEventPublisher implements AuditLogEventPublisher {
    private final RStream<String, String> stream;
    private final ObjectMapper objectMapper;
    private final AuditLogMetrics metrics;

    public RedisAuditLogEventPublisher(RedissonClient redissonClient, ObjectMapper objectMapper,
            AuditStreamProperties properties) {
        this(redissonClient, objectMapper, properties,
                new AuditLogMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    @Autowired
    public RedisAuditLogEventPublisher(RedissonClient redissonClient, AuditStreamProperties properties,
            AuditLogMetrics metrics) {
        this(redissonClient, JsonMapper.builder().addModule(new JavaTimeModule()).build(), properties, metrics);
    }

    public RedisAuditLogEventPublisher(RedissonClient redissonClient, ObjectMapper objectMapper,
            AuditStreamProperties properties, AuditLogMetrics metrics) {
        this.stream = redissonClient.getStream(properties.getStreamKey(), StringCodec.INSTANCE);
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    public void publish(SysOperLog log) {
        try {
            String payload = objectMapper.writeValueAsString(log);
            stream.add(StreamAddArgs.entry("payload", payload));
            metrics.published();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("操作日志事件序列化失败", exception);
        }
    }
}
