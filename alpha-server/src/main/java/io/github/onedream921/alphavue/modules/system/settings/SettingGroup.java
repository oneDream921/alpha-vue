package io.github.onedream921.alphavue.modules.system.settings;

import java.util.Map;
import java.util.Set;

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

    SettingGroup(Set<String> values, Set<String> secrets) {
        this.values = values;
        this.secrets = secrets;
    }

    public void validate(Map<String, ?> input) {
        if (input == null || input.keySet().stream().anyMatch(key -> !values.contains(key) && !secrets.contains(key))) {
            throw new IllegalArgumentException("Unsupported system setting field");
        }
    }

    public boolean isSecret(String key) { return secrets.contains(key); }
    public static SettingGroup parse(String value) { return valueOf(value.replace('-', '_').toUpperCase(java.util.Locale.ROOT)); }
}
