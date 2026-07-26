package io.github.onedream921.alphavue.modules.file.service;

import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * 为私有文件生成和校验短期访问签名。
 */
@Service
public class FileAccessTokenService {
    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final FileStorageProperties properties;
    private final Clock clock;

    @Autowired
    public FileAccessTokenService(FileStorageProperties properties) {
        this(properties, Clock.systemUTC());
    }

    FileAccessTokenService(FileStorageProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String accessUrl(long fileId) {
        if (properties.isPublicAccess()) {
            throw new IllegalStateException("Public files do not need an access token");
        }
        long expiresAt = Instant.now(clock).plus(properties.getAccessTokenTtl()).getEpochSecond();
        return "/api/files/" + fileId + "/content?expires=" + expiresAt + "&signature=" + signature(fileId, expiresAt);
    }

    public boolean isValid(long fileId, long expiresAt, String signature) {
        if (properties.isPublicAccess() || signature == null || expiresAt < Instant.now(clock).getEpochSecond()) {
            return false;
        }
        return MessageDigest.isEqual(signature(fileId, expiresAt).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    private String signature(long fileId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(properties.getAccessTokenSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            byte[] digest = mac.doFinal((fileId + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign file access URL", exception);
        }
    }
}
