package io.github.onedream921.alphavue.modules.monitor.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Redis 运维台受控键前缀配置
 */
@Component
@ConfigurationProperties(prefix = "alpha.redis-management")
public class RedisManagementProperties {
    private List<String> prefixes = List.of("auth:", "satoken:");

    public List<String> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
        this.prefixes = prefixes;
    }

    /**
     * 验证受控前缀，避免错误配置扩大管理范围
     */
    @PostConstruct
    void validatePrefixes() {
        Set<String> normalized = new LinkedHashSet<>();
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isBlank() || !prefix.endsWith(":")) {
                throw new IllegalStateException("Redis 运维前缀配置无效");
            }
            normalized.add(prefix);
        }
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Redis 运维前缀配置不能为空");
        }
        prefixes = List.copyOf(normalized);
    }
}
