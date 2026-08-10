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
        assertThat(response.question()).isNull();
        assertThat(response.image()).startsWith("data:image/png;base64,");
        assertThat(store.code).matches("\\d{6}");
        service.validate(response.captchaId(), store.code);
        assertThatThrownBy(() -> service.validate(response.captchaId(), store.code))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createsMathCaptchaFromLoginSettings() {
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
        assertThat(response.sliderBackground()).startsWith("data:image/jpg;base64,");
        assertThat(response.sliderPiece()).startsWith("data:image/png;base64,");
        assertThat(response.sliderWidth()).isEqualTo(420);
        assertThat(response.image()).isNull();
        String target = store.code.substring("slider:".length());
        service.validate(response.captchaId(), target + "~900~0,20,0;80,22,220;160,24,480;220,24,700;"
                + (Integer.parseInt(target) - 80) + ",24,820;" + target + ",24,900");
    }

    @Test
    void rejectsSliderPayloadWhenTraceDoesNotEndAtSubmittedPosition() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setCaptchaEnabled(true);
        CapturingStore store = new CapturingStore();
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.get(SettingGroup.LOGIN)).thenReturn(new SystemSettingVo(
                "login", Map.of("captchaType", "slider"), Map.of(), false));
        CaptchaService service = new CaptchaService(properties, store, settings);

        CaptchaService.CaptchaResponse response = service.create();
        String target = store.code.substring("slider:".length());

        assertThatThrownBy(() -> service.validate(response.captchaId(),
                target + "~900~0,20,0;80,22,220;160,24,480;220,24,700;0,24,900"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsSliderPayloadOutsideTrackBounds() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setCaptchaEnabled(true);
        CapturingStore store = new CapturingStore();
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.get(SettingGroup.LOGIN)).thenReturn(new SystemSettingVo(
                "login", Map.of("captchaType", "slider"), Map.of(), false));
        CaptchaService service = new CaptchaService(properties, store, settings);

        CaptchaService.CaptchaResponse response = service.create();
        String target = store.code.substring("slider:".length());

        assertThatThrownBy(() -> service.validate(response.captchaId(),
                target + "~900~0,20,0;80,22,220;160,24,480;220,24,700;" + target + ",24,9000"))
                .isInstanceOf(BusinessException.class);
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
