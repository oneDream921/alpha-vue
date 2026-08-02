package io.github.onedream921.alphavue.modules.log.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/** 对审计摘要执行结构化清理、限深和限长。 */
@Component
public class AuditDetailSanitizer {
    private static final int MAX_DEPTH = 6;
    private static final int MAX_STRING = 256;
    private static final int MAX_ITEMS = 50;
    private static final Set<String> SENSITIVE_NAMES = Set.of(
            "password", "passwd", "token", "access_token", "refresh_token", "cookie", "authorization",
            "captcha", "secret", "privatekey", "private_key", "apikey", "api_key", "credential", "key");

    private final ObjectMapper objectMapper;

    @Autowired
    public AuditDetailSanitizer() {
        this(JsonMapper.builder().addModule(new JavaTimeModule()).build());
    }

    public AuditDetailSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    public String request(Object[] arguments) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode values = root.putArray("arguments");
            for (Object argument : arguments) {
                if (argument instanceof jakarta.servlet.ServletRequest || argument instanceof jakarta.servlet.ServletResponse
                        || argument instanceof org.springframework.web.multipart.MultipartFile) {
                    values.add("[omitted]");
                } else {
                    values.add(sanitize(objectMapper.valueToTree(argument), 0, null));
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (RuntimeException | IOException ignored) {
            return null;
        }
    }

    public String response(Object result) {
        try {
            JsonNode node = objectMapper.valueToTree(result);
            ObjectNode summary = objectMapper.createObjectNode();
            summary.put("captured", true);
            summary.put("type", result == null ? "null" : result.getClass().getSimpleName());
            JsonNode data = node == null ? null : node.get("data");
            if (data != null && data.isArray()) summary.put("dataKind", "list").put("itemCount", data.size());
            else if (data != null && data.isObject()) summary.put("dataKind", "object");
            else if (data != null) summary.put("dataKind", data.getNodeType().name().toLowerCase(Locale.ROOT));
            return objectMapper.writeValueAsString(summary);
        } catch (RuntimeException | IOException ignored) {
            return null;
        }
    }

    private JsonNode sanitize(JsonNode node, int depth, String fieldName) {
        if (node == null || node.isNull()) return node;
        if (fieldName != null && sensitive(fieldName)) return objectMapper.getNodeFactory().textNode("[redacted]");
        if (depth >= MAX_DEPTH) return objectMapper.getNodeFactory().textNode("[truncated]");
        if (node.isTextual()) return objectMapper.getNodeFactory().textNode(limit(node.textValue()));
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> result.set(entry.getKey(), sanitize(entry.getValue(), depth + 1, entry.getKey())));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            int count = 0;
            for (JsonNode item : node) {
                if (count++ == MAX_ITEMS) { result.add("[truncated]"); break; }
                result.add(sanitize(item, depth + 1, null));
            }
            return result;
        }
        return node;
    }

    private static boolean sensitive(String name) {
        String normalized = name.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_NAMES.stream().anyMatch(normalized::contains);
    }

    private static String limit(String value) {
        return value.length() <= MAX_STRING ? value : value.substring(0, MAX_STRING) + "...[truncated]";
    }
}
