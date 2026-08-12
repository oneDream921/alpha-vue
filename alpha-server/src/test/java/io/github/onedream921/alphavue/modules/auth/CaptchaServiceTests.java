package io.github.onedream921.alphavue.modules.auth;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.modules.auth.config.CaptchaProperties;
import io.github.onedream921.alphavue.modules.auth.service.CaptchaService;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import io.github.onedream921.alphavue.modules.system.settings.vo.SystemSettingVo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptchaServiceTests {

    @Test
    void createsAndConsumesCaptchaExactlyOnce() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setCaptchaEnabled(true);
        CapturingStore store = new CapturingStore();
        CaptchaService service = new CaptchaService(properties, store);

        CaptchaService.CaptchaResponse response = service.create();

        assertThat(response.enabled()).isTrue();
        assertThat(response.type()).isEqualTo("numeric");
        assertThat(response.image()).startsWith("data:image/png;base64,");
        assertThat(store.code).matches("\\d{4}");
        service.validate(response.captchaId(), store.code, null);
        assertThatThrownBy(() -> service.validate(response.captchaId(), store.code, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reportsSliderCaptchaFromLoginSettings() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setCaptchaEnabled(true);
        CapturingStore store = new CapturingStore();
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.get(SettingGroup.LOGIN)).thenReturn(new SystemSettingVo(
                "login", Map.of("captchaType", "slider", "rememberMeEnabled", false), Map.of(), false));
        CaptchaService service = new CaptchaService(properties, store, settings);

        CaptchaService.CaptchaResponse response = service.create();

        assertThat(response.type()).isEqualTo("slider");
        assertThat(response.rememberMeEnabled()).isFalse();
        assertThat(response.image()).isNull();
    }

    private static final class CapturingStore implements CaptchaService.CaptchaStore {
        private String id;
        private String code;
        @Override public void put(String id, String code, Duration ttl) { this.id = id; this.code = code; }
        @Override public String consume(String id) {
            if (!id.equals(this.id)) return null;
            String current = code;
            this.id = null;
            return current;
        }
    }
}
