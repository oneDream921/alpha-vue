package io.github.onedream921.alphavue.modules.log.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 审计 Stream 消费调度开关。 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(AuditStreamProperties.class)
@ConditionalOnProperty(prefix = "alpha.log.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditStreamSchedulingConfiguration {
}
