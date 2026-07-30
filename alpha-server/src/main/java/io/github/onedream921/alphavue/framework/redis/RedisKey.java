package io.github.onedream921.alphavue.framework.redis;

import java.util.Arrays;
import java.util.Objects;

/**
 * Alpha Redis 键规则。
 *
 * <p>只允许由领域、用途和标识组成的受控键，避免业务代码自行拼接跨域键。</p>
 */
public record RedisKey(String value) {

    public RedisKey {
        Objects.requireNonNull(value, "Redis key must not be null");
        String[] parts = value.split(":", -1);
        if (parts.length != 4
                || !"alpha".equals(parts[0])
                || Arrays.stream(parts, 1, parts.length).anyMatch(RedisKey::invalidSegment)) {
            throw new IllegalArgumentException("Redis key must use alpha namespace");
        }
    }

    public static RedisKey of(String domain, String purpose, String identifier) {
        return new RedisKey("alpha:" + segment(domain) + ':' + segment(purpose) + ':' + segment(identifier));
    }

    private static String segment(String value) {
        if (invalidSegment(value)) {
            throw new IllegalArgumentException("Redis key segment is invalid");
        }
        return value;
    }

    private static boolean invalidSegment(String value) {
        return value == null || value.isBlank() || value.indexOf(':') >= 0 || value.indexOf('*') >= 0;
    }
}
