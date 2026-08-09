package io.github.onedream921.alphavue.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XssSanitizingFilterTests {
    @Test
    void escapesJsonTextWhenEnabled() throws Exception {
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.SECURITY)).thenReturn(Map.of("xssFilteringEnabled", true));
        MockHttpServletRequest request = request("{\"name\":\"<script>alert(1)</script>\"}");
        AtomicReference<String> observed = new AtomicReference<>();

        new XssSanitizingFilter(settings, new ObjectMapper()).doFilter(request, new MockHttpServletResponse(),
                (wrapped, response) -> observed.set(new String(wrapped.getInputStream().readAllBytes(), StandardCharsets.UTF_8)));

        assertThat(observed.get()).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void preservesJsonTextWhenDisabled() throws Exception {
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.SECURITY)).thenReturn(Map.of("xssFilteringEnabled", false));
        MockHttpServletRequest request = request("{\"name\":\"<b>safe</b>\"}");
        AtomicReference<String> observed = new AtomicReference<>();

        new XssSanitizingFilter(settings, new ObjectMapper()).doFilter(request, new MockHttpServletResponse(),
                (wrapped, response) -> observed.set(new String(wrapped.getInputStream().readAllBytes(), StandardCharsets.UTF_8)));

        assertThat(observed.get()).isEqualTo("{\"name\":\"<b>safe</b>\"}");
    }

    private static MockHttpServletRequest request(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/system/settings/site");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
