package io.github.onedream921.alphavue.modules.auth.service;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.auth.config.CaptchaProperties;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class CaptchaService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String SLIDER = "slider";
    private static final String BLOCK_PUZZLE = "blockPuzzle";
    private final CaptchaProperties properties;
    private final CaptchaStore store;
    private final SystemSettingService settingService;
    private final com.anji.captcha.service.CaptchaService ajCaptchaService;

    public CaptchaService(CaptchaProperties properties, CaptchaStore store) { this(properties, store, null, null); }
    public CaptchaService(CaptchaProperties properties, CaptchaStore store, SystemSettingService settings) {
        this(properties, store, settings, null);
    }
    @Autowired
    public CaptchaService(CaptchaProperties properties, CaptchaStore store, SystemSettingService settings,
                          com.anji.captcha.service.CaptchaService ajCaptchaService) {
        this.properties = properties;
        this.store = store;
        this.settingService = settings;
        this.ajCaptchaService = ajCaptchaService;
    }

    public CaptchaResponse create() {
        String type = captchaType();
        if (!captchaEnabled()) return new CaptchaResponse(false, type, rememberMeEnabled(), null, null);
        if (SLIDER.equals(type)) return new CaptchaResponse(true, type, rememberMeEnabled(), null, null);
        String id = UUID.randomUUID().toString();
        SpecCaptcha captcha = new SpecCaptcha(128, 40, 4);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        String code = captcha.text();
        store.put(id, code, properties.getCaptchaTtl());
        return new CaptchaResponse(true, type, rememberMeEnabled(), id, captcha.toBase64());
    }

    public ResponseModel sliderGet(CaptchaVO request) {
        requireSlider();
        request.setCaptchaType(BLOCK_PUZZLE);
        long startedAt = System.nanoTime();
        ResponseModel response = ajCaptchaService.get(request);
        log.info("slider captcha get: repCode={}, repMsg={}, success={}, clientUidPresent={}, clientUidHash={}, elapsedMs={}",
                response.getRepCode(), response.getRepMsg(), response.isSuccess(), present(request.getClientUid()),
                digest(request.getClientUid()), elapsedMs(startedAt));
        return response;
    }

    public ResponseModel sliderCheck(CaptchaVO request) {
        requireSlider();
        request.setCaptchaType(BLOCK_PUZZLE);
        long startedAt = System.nanoTime();
        log.info("slider captcha check received: tokenPresent={}, tokenHash={}, pointJsonPresent={}, pointJsonLength={}, "
                        + "clientUidPresent={}, clientUidHash={}",
                present(request.getToken()), digest(request.getToken()), present(request.getPointJson()),
                length(request.getPointJson()), present(request.getClientUid()), digest(request.getClientUid()));
        ResponseModel response = ajCaptchaService.check(request);
        String level = response.isSuccess() ? "info" : "warn";
        logAt(level, "slider captcha check result: repCode={}, repMsg={}, success={}, tokenHash={}, clientUidHash={}, elapsedMs={}",
                response.getRepCode(), response.getRepMsg(), response.isSuccess(), digest(request.getToken()),
                digest(request.getClientUid()), elapsedMs(startedAt));
        return response;
    }

    public void validate(String id, String code, String sliderVerification) {
        if (!captchaEnabled()) return;
        if (SLIDER.equals(captchaType())) {
            CaptchaVO request = new CaptchaVO();
            request.setCaptchaType(BLOCK_PUZZLE);
            request.setCaptchaVerification(sliderVerification);
            ResponseModel response = ajCaptchaService == null ? null : ajCaptchaService.verification(request);
            log.info("slider captcha verify: repCode={}, verificationPresent={}",
                    response == null ? "service-unavailable" : response.getRepCode(), sliderVerification != null && !sliderVerification.isBlank());
            if (response == null || !response.isSuccess()) invalid();
            return;
        }
        String expected = id == null ? null : store.consume(id);
        if (expected == null || code == null || !expected.equals(code.trim())) invalid();
    }

    private void requireSlider() {
        if (!captchaEnabled() || !SLIDER.equals(captchaType()) || ajCaptchaService == null) invalid();
    }
    private static void invalid() { throw new BusinessException(401, PublicErrorMessage.INVALID_CREDENTIALS); }
    private String captchaType() {
        Object value = setting("captchaType");
        return SLIDER.equals(value) ? SLIDER : "numeric";
    }
    private boolean captchaEnabled() {
        Object value = setting("captchaEnabled");
        return value instanceof Boolean bool ? bool : value instanceof String text ? Boolean.parseBoolean(text) : properties.isCaptchaEnabled();
    }
    private boolean rememberMeEnabled() {
        Object value = setting("rememberMeEnabled");
        return value instanceof Boolean bool ? bool : !(value instanceof String text) || Boolean.parseBoolean(text);
    }
    private Object setting(String key) {
        return settingService == null ? null : settingService.get(SettingGroup.LOGIN).values().get(key);
    }

    private static String digest(String value) {
        if (value == null || value.isBlank()) return "-";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(16);
            for (int i = 0; i < 8; i++) result.append(String.format("%02x", hash[i]));
            return result.toString();
        } catch (Exception exception) {
            return "unavailable";
        }
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }
    private static int length(String value) { return value == null ? 0 : value.length(); }
    private static long elapsedMs(long startedAt) { return (System.nanoTime() - startedAt) / 1_000_000; }

    private static void logAt(String level, String pattern, Object... arguments) {
        if ("warn".equals(level)) log.warn(pattern, arguments);
        else log.info(pattern, arguments);
    }

    public interface CaptchaStore {
        void put(String id, String code, Duration ttl);
        String consume(String id);
    }
    public record CaptchaResponse(boolean enabled, String type, boolean rememberMeEnabled, String captchaId, String image) { }
}
