package io.github.onedream921.alphavue.modules.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.modules.log.service.AuditDetailSanitizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDetailSanitizerTests {
    private final AuditDetailSanitizer sanitizer = new AuditDetailSanitizer(new ObjectMapper());

    @Test
    void recursivelyRedactsSensitiveFieldsAndBoundsText() {
        String value = sanitizer.request(new Object[]{Map.of("username", "admin", "password", "secret",
                "profile", Map.of("api_key", "credential"))});

        assertThat(value).contains("\"username\":\"admin\"")
                .contains("\"password\":\"[redacted]\"")
                .contains("\"api_key\":\"[redacted]\"")
                .doesNotContain("secret", "credential");
    }

    @Test
    void responseOnlyStoresShapeAndCount() {
        String value = sanitizer.response(Map.of("code", 200, "data", java.util.List.of(Map.of("token", "hidden"))));

        assertThat(value).contains("\"dataKind\":\"list\"")
                .contains("\"itemCount\":1")
                .doesNotContain("hidden", "token");
    }

    @Test
    void responseSupportsJavaTimeFieldsInUserLikeResponses() {
        String value = sanitizer.response(Map.of(
                "code", 200,
                "data", Map.of("username", "alice", "createdAt", LocalDateTime.now())));

        assertThat(value).contains("\"captured\":true", "\"dataKind\":\"object\"");
    }
}
