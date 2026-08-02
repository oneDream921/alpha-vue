package io.github.onedream921.alphavue.framework.scheduling;

import com.aizuda.snailjob.client.starter.EnableSnailJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/**
 * Enables the optional SnailJob client only when explicitly configured.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "snail-job", name = "enabled", havingValue = "true")
@EnableSnailJob
public class SnailJobConfiguration {

    private final Environment environment;

    public SnailJobConfiguration(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateToken() {
        if (!StringUtils.hasText(environment.getProperty("snail-job.token"))) {
            throw new IllegalStateException("snail-job.token must be configured when SnailJob is enabled");
        }
    }
}
