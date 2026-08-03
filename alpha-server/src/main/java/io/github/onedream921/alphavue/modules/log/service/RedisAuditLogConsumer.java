package io.github.onedream921.alphavue.modules.log.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.onedream921.alphavue.modules.log.config.AuditStreamProperties;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.PendingEntry;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Redis Streams 操作日志 Consumer：数据库提交成功后才 ACK。 */
@Component
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnProperty(prefix = "alpha.log.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAuditLogConsumer {
    private static final Logger log = LoggerFactory.getLogger(RedisAuditLogConsumer.class);

    private final RStream<String, String> stream;
    private final RStream<String, String> deadLetterStream;
    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;
    private final AuditStreamProperties properties;
    private final String consumerName;
    private final AuditLogMetrics metrics;

    public RedisAuditLogConsumer(RedissonClient redissonClient, SysOperLogMapper operLogMapper,
            ObjectMapper objectMapper, AuditStreamProperties properties) {
        this(redissonClient, operLogMapper, objectMapper, properties,
                new AuditLogMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    @Autowired
    public RedisAuditLogConsumer(RedissonClient redissonClient, SysOperLogMapper operLogMapper,
            AuditStreamProperties properties, AuditLogMetrics metrics) {
        this(redissonClient, operLogMapper, JsonMapper.builder().addModule(new JavaTimeModule()).build(), properties,
                metrics);
    }

    public RedisAuditLogConsumer(RedissonClient redissonClient, SysOperLogMapper operLogMapper,
            ObjectMapper objectMapper, AuditStreamProperties properties, AuditLogMetrics metrics) {
        this.stream = redissonClient.getStream(properties.getStreamKey(), StringCodec.INSTANCE);
        this.deadLetterStream = redissonClient.getStream(properties.getDeadLetterStreamKey(), StringCodec.INSTANCE);
        this.operLogMapper = operLogMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
        this.consumerName = properties.getConsumerName() == null || properties.getConsumerName().isBlank()
                ? "alpha-audit-" + UUID.randomUUID() : properties.getConsumerName();
    }

    @PostConstruct
    void ensureConsumerGroup() {
        try {
            stream.createGroup(StreamCreateGroupArgs.name(properties.getConsumerGroup())
                    .entriesRead(0).makeStream());
            stream.createConsumer(properties.getConsumerGroup(), consumerName);
        } catch (RuntimeException exception) {
            log.debug("审计 Stream Consumer Group 已存在或当前 Redis 暂不可用，group={}",
                    properties.getConsumerGroup(), exception);
        }
    }

    @Scheduled(fixedDelayString = "${alpha.log.stream.poll-delay-ms:1000}",
            initialDelayString = "${alpha.log.stream.initial-delay-ms:1000}")
    public void consume() {
        try {
            recoverPending();
            Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
                    properties.getConsumerGroup(), consumerName,
                    StreamReadGroupArgs.neverDelivered()
                            .count(properties.getBatchSize())
                            .timeout(Duration.ofMillis(properties.getReadTimeoutMs())));
            if (messages != null) {
                messages.forEach(this::process);
            }
        } catch (RuntimeException exception) {
            log.warn("审计 Stream 消费失败，group={}", properties.getConsumerGroup(), exception);
        }
    }

    private void recoverPending() {
        List<PendingEntry> pending = stream.listPending(properties.getConsumerGroup(), StreamMessageId.MIN,
                StreamMessageId.MAX, properties.getBatchSize());
        for (PendingEntry entry : pending) {
            if (entry.getIdleTime() < properties.getClaimIdleMs()) continue;
            Map<StreamMessageId, Map<String, String>> claimed = stream.claim(properties.getConsumerGroup(),
                    consumerName, properties.getClaimIdleMs(), TimeUnit.MILLISECONDS, entry.getId());
            claimed.forEach((id, values) -> {
                if (entry.getDeliveryCount() >= properties.getMaxDeliveryCount()) {
                    moveToDeadLetter(id, values);
                } else {
                    process(id, values);
                }
            });
        }
    }

    private void process(StreamMessageId id, Map<String, String> values) {
        String payload = values.get("payload");
        if (payload == null || payload.isBlank()) {
            log.warn("审计 Stream 消息缺少 payload，messageId={}", id);
            stream.ack(properties.getConsumerGroup(), id);
            return;
        }
        try {
            SysOperLog logEntry = objectMapper.readValue(payload, SysOperLog.class);
            try {
                operLogMapper.insert(logEntry);
            } catch (RuntimeException exception) {
                if (!isDuplicateEvent(exception)) throw exception;
            }
            stream.ack(properties.getConsumerGroup(), id);
            metrics.consumed();
        } catch (JsonProcessingException exception) {
            log.error("审计 Stream 消息格式无效，转入死信，messageId={}", id, exception);
            moveToDeadLetter(id, values);
        } catch (RuntimeException exception) {
            metrics.failed();
            log.warn("审计日志数据库写入失败，保留 Pending 等待重试，messageId={}", id, exception);
        }
    }

    private void moveToDeadLetter(StreamMessageId id, Map<String, String> values) {
        try {
            deadLetterStream.add(org.redisson.api.stream.StreamAddArgs.entries(
                    "originalMessageId", id.toString(), "payload", values.getOrDefault("payload", "")));
            stream.ack(properties.getConsumerGroup(), id);
            metrics.deadLetter();
            log.error("审计日志达到最大重试次数，已转入死信，messageId={}", id);
        } catch (RuntimeException exception) {
            log.error("审计日志转入死信失败，继续保留 Pending，messageId={}", id, exception);
        }
    }

    private static boolean isDuplicateEvent(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && (message.contains("Duplicate entry") || message.contains("duplicate key"));
    }
}
