package io.github.onedream921.alphavue.modules.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.modules.log.config.AuditStreamProperties;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.service.RedisAuditLogEventPublisher;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAuditLogEventPublisherTests {
    @Test
    void publishesSanitizedOperationEventAsStreamPayload() {
        RedissonClient client = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        doReturn(stream).when(client).getStream("alpha:audit:operation:v1", StringCodec.INSTANCE);
        AuditStreamProperties properties = new AuditStreamProperties();
        RedisAuditLogEventPublisher publisher = new RedisAuditLogEventPublisher(client, new ObjectMapper(), properties);

        SysOperLog log = new SysOperLog();
        log.setEventId("event-1");
        log.setOperation("Create department");
        log.setRequestParams("[redacted]");
        publisher.publish(log);

        verify(stream).add(any(StreamAddArgs.class));
    }
}
