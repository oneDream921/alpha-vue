package io.github.onedream921.alphavue.framework.redis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Maps untrusted logical identifiers to stable, bounded Redis identifiers. */
public final class RedisPhysicalKey {
    private RedisPhysicalKey() {
    }

    public static RedisKey forIdentifier(String domain, String purpose, String identifier) {
        return RedisKey.of(domain, purpose, sha256(identifier));
    }

    public static String sha256(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Redis logical identifier must not be null");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
