package io.github.onedream921.alphavue.modules.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Redis Streams 审计事件投递配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "alpha.log.stream")
public class AuditStreamProperties {
    private boolean enabled = true;
    private String streamKey = "alpha:audit:operation:v1";
    private String deadLetterStreamKey = "alpha:audit:operation:dlq:v1";
    private String consumerGroup = "alpha-audit-db";
    private String consumerName = "";
    private int batchSize = 100;
    private long readTimeoutMs = 1000;
    private long claimIdleMs = 60_000;
    private int maxDeliveryCount = 10;
}
