package io.github.onedream921.alphavue.modules.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.modules.log.config.AuditStreamProperties;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import io.github.onedream921.alphavue.modules.log.service.RedisAuditLogConsumer;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAuditLogConsumerTests {
    @Test
    void acknowledgesOnlyAfterDatabaseInsert() throws Exception {
        RedissonClient client = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> deadLetterStream = mock(RStream.class);
        AuditStreamProperties properties = new AuditStreamProperties();
        doReturn(stream).when(client).getStream(eq(properties.getStreamKey()), eq(StringCodec.INSTANCE));
        doReturn(deadLetterStream).when(client).getStream(eq(properties.getDeadLetterStreamKey()),
                eq(StringCodec.INSTANCE));
        StreamMessageId messageId = new StreamMessageId(1, 0);
        SysOperLog log = new SysOperLog();
        log.setEventId("event-1");
        log.setOperation("Create department");
        String payload = new ObjectMapper().writeValueAsString(log);
        when(stream.listPending(anyString(), any(StreamMessageId.class), any(StreamMessageId.class), eq(100)))
                .thenReturn(java.util.List.of());
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class))).thenReturn(
                Map.of(messageId, Map.of("payload", payload)));
        SysOperLogMapper mapper = mock(SysOperLogMapper.class);
        RedisAuditLogConsumer consumer = new RedisAuditLogConsumer(client, mapper, new ObjectMapper(), properties);
        consumer.consume();

        verify(mapper).insert(any(SysOperLog.class));
        verify(stream).ack(properties.getConsumerGroup(), messageId);
    }
}
