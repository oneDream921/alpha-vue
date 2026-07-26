package io.github.onedream921.alphavue.modules.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 运维台安全配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.monitor.redis")
public class RedisManagementProperties {

    /**
     * 是否对验证码、会话和疑似密钥等敏感键的值进行脱敏。
     */
    private boolean maskValues = true;
}
