package io.github.onedream921.alphavue.modules.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 审计日志运行配置。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "alpha.log")
public class AuditLogProperties {
    /** 是否为预期业务异常记录调用堆栈，默认关闭。 */
    private boolean captureBusinessExceptionStack;
}
