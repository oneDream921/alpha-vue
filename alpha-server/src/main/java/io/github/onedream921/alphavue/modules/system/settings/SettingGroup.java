package io.github.onedream921.alphavue.modules.system.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.net.URI;

/** Compile-time registry; administrators cannot create arbitrary technical settings. */
public enum SettingGroup {
    SITE(Set.of("siteName", "siteDescription", "siteLogo", "copyright", "icp", "watermarkEnabled", "watermarkType", "watermarkContent", "watermarkOpacity"), Set.of()),
    LOGIN(Set.of("captchaEnabled", "captchaType", "maxRetry", "lockMinutes", "rememberMeEnabled"), Set.of()),
    FILE(Set.of("provider", "accessDomain", "endpoint", "bucket", "region", "maxSizeMb", "allowedExtensions", "storagePath", "publicAccess", "privateAccessTtlMinutes"), Set.of("accessKey", "secretKey")),
    OAUTH(Set.of("wechatEnabled", "alipayEnabled", "githubEnabled", "callbackBaseUrl"), Set.of("wechatAppId", "wechatAppSecret", "alipayAppId", "alipayAppSecret", "githubClientId", "githubClientSecret")),
    PAYMENT(Set.of("wechatEnabled", "alipayEnabled", "wechatMerchantId", "wechatNotifyUrl", "alipayAppId", "alipayNotifyUrl"), Set.of("wechatApiV3Key", "wechatPrivateKey", "alipayPrivateKey", "alipayPublicKey")),
    SECURITY(Set.of("xssFilteringEnabled", "rsaPublicKey"), Set.of("rsaPrivateKey")),
    MINI_PROGRAM(Set.of("appId"), Set.of("appSecret")),
    OFFICIAL_ACCOUNT(Set.of("appId", "callbackUrl", "oauthCallbackUrl", "customMenuJson"), Set.of("appSecret", "token", "encodingAesKey"));

    private final Set<String> values;
    private final Set<String> secrets;
    private static final ObjectMapper JSON = new ObjectMapper();

    SettingGroup(Set<String> values, Set<String> secrets) {
        this.values = values;
        this.secrets = secrets;
    }

    public void validate(Map<String, ?> input) {
        if (input == null || input.keySet().stream().anyMatch(key -> !values.contains(key) && !secrets.contains(key))) {
            throw new IllegalArgumentException("Unsupported system setting field");
        }
        input.forEach((key, value) -> validateValue(key, value));
    }

    private void validateValue(String key, Object value) {
        if (value == null) return;
        if (key.endsWith("Enabled") || key.equals("publicAccess") || key.equals("xssFilteringEnabled")) {
            require(value instanceof Boolean); return;
        }
        if (key.equals("maxRetry")) { requireInteger(value, 1, 20); return; }
        if (key.equals("lockMinutes")) { requireInteger(value, 1, 1_440); return; }
        if (key.equals("maxSizeMb") || key.equals("privateAccessTtlMinutes")) { requireInteger(value, 1, 10_080); return; }
        if (key.equals("watermarkOpacity")) {
            require(value instanceof Number number && number.doubleValue() >= 0.05 && number.doubleValue() <= 0.5); return;
        }
        if (key.equals("provider")) { requireOneOf(value, "local", "minio", "oss", "cos"); return; }
        if (key.equals("captchaType")) { requireOneOf(value, "numeric", "slider"); return; }
        if (key.equals("watermarkType")) { requireOneOf(value, "custom", "username"); return; }
        require(value instanceof String);
        String text = ((String) value).trim();
        require(text.length() <= (key.endsWith("Key") || key.toLowerCase().contains("private") || key.equals("customMenuJson") ? 16_384 : 1_024));
        if (Set.of("callbackBaseUrl", "wechatNotifyUrl", "alipayNotifyUrl", "callbackUrl", "oauthCallbackUrl", "endpoint").contains(key)
                && !text.isEmpty()) requireHttpUrl(text);
        if (key.equals("accessDomain") && !text.isEmpty() && !text.startsWith("/")) requireHttpUrl(text);
        if (key.equals("allowedExtensions") && !text.isEmpty()) {
            require(text.split(",").length <= 50);
            for (String extension : text.split(",")) require(extension.trim().matches("[A-Za-z0-9]{1,16}"));
        }
        if (key.equals("customMenuJson") && !text.isEmpty()) {
            try { require(JSON.readTree(text) != null); }
            catch (Exception exception) { throw new IllegalArgumentException("Invalid JSON", exception); }
        }
    }

    private static void requireHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            require(Set.of("http", "https").contains(uri.getScheme()) && uri.getHost() != null);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid URL", exception);
        }
    }

    private static void requireOneOf(Object value, String... allowed) {
        require(value instanceof String && Set.of(allowed).contains(value));
    }

    private static void requireInteger(Object value, int min, int max) {
        require(value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())
                && number.longValue() >= min && number.longValue() <= max);
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("Invalid system setting value");
    }

    public boolean isSecret(String key) { return secrets.contains(key); }
    public static SettingGroup parse(String value) { return valueOf(value.replace('-', '_').toUpperCase(java.util.Locale.ROOT)); }
}
