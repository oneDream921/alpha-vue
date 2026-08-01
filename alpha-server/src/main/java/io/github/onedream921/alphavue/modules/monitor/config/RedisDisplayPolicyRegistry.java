package io.github.onedream921.alphavue.modules.monitor.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Redis 缓存展示策略注册表。
 *
 * <p>只有注册的命名空间才允许参与分级策略；未识别的键默认脱敏。</p>
 */
@Component
public class RedisDisplayPolicyRegistry {
    private final List<Definition> definitions = List.of(
            new Definition("captcha", "验证码", List.of("alpha:auth:captcha:", "auth:captcha:"), RedisDisplayLevel.HIDDEN, true),
            new Definition("login-failure", "登录失败窗口", List.of("alpha:auth:login-failure:", "auth:login:failure:"), RedisDisplayLevel.HIDDEN, true),
            new Definition("session", "Sa-Token 会话", List.of("alpha:sa-token:", "satoken:", "authorization:"), RedisDisplayLevel.HIDDEN, true),
            new Definition("dictionary", "数据字典缓存", List.of("alpha:system:cache:dictionary", "system:dict:"), RedisDisplayLevel.MASKED, false),
            new Definition("business", "业务/缓存数据", List.of(), RedisDisplayLevel.MASKED, false)
    );

    public Definition resolve(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return definitions.stream()
                .filter(definition -> definition.prefixes().stream().anyMatch(normalized::startsWith))
                .findFirst()
                .orElse(definitions.get(definitions.size() - 1));
    }

    public String configKey(Definition definition) {
        return "cache.display." + definition.id();
    }

    public record Definition(String id, String category, List<String> prefixes, RedisDisplayLevel defaultLevel,
                             boolean sensitive) {
        public Definition {
            prefixes = List.copyOf(prefixes);
        }
    }
}
