package io.github.onedream921.alphavue.modules.auth;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.modules.auth.config.CaptchaProperties;
import io.github.onedream921.alphavue.modules.auth.service.CaptchaService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaptchaServiceTests {

    @Test
    void createsAndConsumesCaptchaExactlyOnce() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setCaptchaEnabled(true);
        CapturingStore store = new CapturingStore();
        CaptchaService service = new CaptchaService(properties, store);

        CaptchaService.CaptchaResponse response = service.create();

        assertThat(response.enabled()).isTrue();
        assertThat(response.image()).startsWith("data:image/png;base64,");
        service.validate(response.captchaId(), store.code);
        assertThatThrownBy(() -> service.validate(response.captchaId(), store.code))
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
