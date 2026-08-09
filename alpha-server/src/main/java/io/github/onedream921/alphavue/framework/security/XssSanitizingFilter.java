package io.github.onedream921.alphavue.framework.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/** Escapes JSON string values only when the runtime security setting enables it. */
public class XssSanitizingFilter extends OncePerRequestFilter {
    private final SystemSettingService settingService;
    private final ObjectMapper objectMapper;

    public XssSanitizingFilter(SystemSettingService settingService, ObjectMapper objectMapper) {
        this.settingService = settingService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled() || !isJson(request)) {
            chain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readAllBytes();
        if (body.length == 0) {
            chain.doFilter(request, response);
            return;
        }
        try {
            JsonNode value = objectMapper.readTree(body);
            escape(value);
            chain.doFilter(new CachedBodyRequest(request, objectMapper.writeValueAsBytes(value)), response);
        } catch (IOException exception) {
            chain.doFilter(new CachedBodyRequest(request, body), response);
        }
    }

    private boolean enabled() {
        Object configured = settingService.runtimeValues(SettingGroup.SECURITY).get("xssFilteringEnabled");
        return configured instanceof Boolean value ? value : Boolean.parseBoolean(String.valueOf(configured));
    }

    private static boolean isJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    private static void escape(JsonNode node) {
        if (node instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getValue().isTextual()) object.put(field.getKey(), HtmlUtils.htmlEscape(field.getValue().textValue()));
                else escape(field.getValue());
            }
        } else if (node instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                JsonNode value = array.get(index);
                if (value.isTextual()) array.set(index, objectText(HtmlUtils.htmlEscape(value.textValue())));
                else escape(value);
            }
        }
    }

    private static JsonNode objectText(String value) {
        return com.fasterxml.jackson.databind.node.TextNode.valueOf(value);
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) { }
            };
        }
    }
}
