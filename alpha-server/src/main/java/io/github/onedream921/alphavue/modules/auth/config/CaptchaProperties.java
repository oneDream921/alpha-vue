package io.github.onedream921.alphavue.modules.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 验证码配置属性
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.auth")
public class CaptchaProperties {
    private boolean captchaEnabled;
    private Duration captchaTtl = Duration.ofMinutes(5);
}
