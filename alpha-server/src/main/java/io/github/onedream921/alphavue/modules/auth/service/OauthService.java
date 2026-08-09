package io.github.onedream921.alphavue.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.github.onedream921.alphavue.modules.auth.entity.SysOauthAccount;
import io.github.onedream921.alphavue.modules.auth.mapper.SysOauthAccountMapper;
import io.github.onedream921.alphavue.modules.system.entity.SysUser;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** OAuth authorization, one-time state validation, and immutable external-subject mapping. */
@Service
public class OauthService {
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final SystemSettingService settingService;
    private final SysOauthAccountMapper accountMapper;
    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, PendingState> pendingStates = new ConcurrentHashMap<>();

    public OauthService(SystemSettingService settingService, SysOauthAccountMapper accountMapper, SysUserMapper userMapper,
                        AuthService authService) {
        this.settingService = settingService;
        this.accountMapper = accountMapper;
        this.userMapper = userMapper;
        this.authService = authService;
    }

    public Authorization begin(String provider) {
        Provider resolved = Provider.parse(provider);
        Map<String, Object> settings = settings();
        requireEnabled(resolved, settings);
        String state = UUID.randomUUID().toString().replace("-", "");
        pendingStates.put(state, new PendingState(resolved, Instant.now().plus(STATE_TTL)));
        pendingStates.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
        String callback = callbackUrl(resolved, settings);
        String appId = text(settings, resolved.appIdField);
        return new Authorization(resolved.value, switch (resolved) {
            case GITHUB -> "https://github.com/login/oauth/authorize?client_id=" + encode(appId)
                    + "&redirect_uri=" + encode(callback) + "&scope=read%3Auser%20user%3Aemail&state=" + encode(state);
            case WECHAT -> "https://open.weixin.qq.com/connect/qrconnect?appid=" + encode(appId)
                    + "&redirect_uri=" + encode(callback) + "&response_type=code&scope=snsapi_login&state=" + encode(state) + "#wechat_redirect";
            case ALIPAY -> "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id=" + encode(appId)
                    + "&scope=auth_user&redirect_uri=" + encode(callback) + "&state=" + encode(state);
        });
    }

    @Transactional
    public Result complete(String provider, String code, String state, String ipAddress, String userAgent, String traceId) {
        Provider resolved = Provider.parse(provider);
        PendingState pending = pendingStates.remove(state == null ? "" : state);
        if (pending == null || pending.provider() != resolved || pending.expiresAt().isBefore(Instant.now()) || code == null || code.isBlank()) {
            throw invalid();
        }
        Map<String, Object> settings = settings();
        requireEnabled(resolved, settings);
        Identity identity = identity(resolved, code, callbackUrl(resolved, settings), settings);
        SysOauthAccount account = accountMapper.selectOne(new LambdaQueryWrapper<SysOauthAccount>()
                .eq(SysOauthAccount::getProvider, resolved.value).eq(SysOauthAccount::getSubject, identity.subject()));
        if (account == null) {
            SysUser user = new SysUser();
            user.setUsername(nextUsername(resolved, identity.subject()));
            user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));
            user.setNickname(trim(identity.displayName(), 64, "待审核"));
            user.setAvatar(trim(identity.avatarUrl(), 255, null));
            user.setStatus(0);
            user.setMustChangePassword(1);
            userMapper.insert(user);
            account = new SysOauthAccount();
            account.setProvider(resolved.value);
            account.setSubject(identity.subject());
            account.setUserId(user.getId());
            account.setDisplayName(trim(identity.displayName(), 128, null));
            account.setAvatarUrl(trim(identity.avatarUrl(), 1024, null));
            accountMapper.insert(account);
            return new Result("PENDING_APPROVAL", null);
        }
        return new Result("AUTHENTICATED", authService.loginFromExternalIdentity(account.getUserId(), ipAddress, userAgent, traceId));
    }

    private Identity identity(Provider provider, String code, String callback, Map<String, Object> settings) {
        return switch (provider) {
            case GITHUB -> github(code, callback, settings);
            case WECHAT -> wechat(code, settings);
            case ALIPAY -> alipay(code, settings);
        };
    }

    private Identity github(String code, String callback, Map<String, Object> settings) {
        JsonNode token = postForm("https://github.com/login/oauth/access_token", Map.of("client_id", text(settings, "githubClientId"),
                "client_secret", text(settings, "githubClientSecret"), "code", code, "redirect_uri", callback));
        JsonNode profile = getJson("https://api.github.com/user", token.path("access_token").asText());
        return new Identity(required(profile, "id"), profile.path("login").asText("GitHub 用户"), profile.path("avatar_url").asText(null));
    }

    private Identity wechat(String code, Map<String, Object> settings) {
        JsonNode token = getJson("https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + encode(text(settings, "wechatAppId"))
                + "&secret=" + encode(text(settings, "wechatAppSecret")) + "&code=" + encode(code) + "&grant_type=authorization_code", null);
        JsonNode profile = getJson("https://api.weixin.qq.com/sns/userinfo?access_token=" + encode(required(token, "access_token"))
                + "&openid=" + encode(required(token, "openid")), null);
        return new Identity(required(profile, "openid"), profile.path("nickname").asText("微信用户"), profile.path("headimgurl").asText(null));
    }

    private Identity alipay(String code, Map<String, Object> settings) {
        Map<String, String> tokenParams = alipayParameters(settings, "alipay.system.oauth.token", Map.of("grant_type", "authorization_code", "code", code));
        JsonNode token = getJson("https://openapi.alipay.com/gateway.do?" + form(tokenParams), null)
                .path("alipay_system_oauth_token_response");
        String accessToken = required(token, "access_token");
        JsonNode profile = getJson("https://openapi.alipay.com/gateway.do?" + form(alipayParameters(settings,
                "alipay.user.info.share", Map.of("auth_token", accessToken))), null)
                .path("alipay_user_info_share_response");
        return new Identity(required(profile, "user_id"), profile.path("nick_name").asText("支付宝用户"), profile.path("avatar").asText(null));
    }

    private Map<String, String> alipayParameters(Map<String, Object> settings, String method, Map<String, String> extra) {
        TreeMap<String, String> values = new TreeMap<>();
        values.put("app_id", text(settings, "alipayAppId"));
        values.put("method", method);
        values.put("charset", "utf-8");
        values.put("sign_type", "RSA2");
        values.put("timestamp", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now()));
        values.put("version", "1.0");
        values.putAll(extra);
        values.put("sign", rsaSign(form(values), text(settings, "alipayAppSecret")));
        return values;
    }

    private String rsaSign(String source, String privateKey) {
        try {
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(source.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) { throw invalid(); }
    }

    private JsonNode postForm(String endpoint, Map<String, String> values) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json").header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(values))).build();
            return parse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } catch (Exception exception) { throw invalid(); }
    }

    private JsonNode getJson(String endpoint, String bearerToken) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
            if (bearerToken != null) request.header("Authorization", "Bearer " + bearerToken);
            return parse(HTTP.send(request.GET().build(), HttpResponse.BodyHandlers.ofString()).body());
        } catch (Exception exception) { throw invalid(); }
    }

    private JsonNode parse(String response) { try { JsonNode node = objectMapper.readTree(response); if (node == null || node.has("error") || node.has("errcode")) throw invalid(); return node; } catch (Exception exception) { throw invalid(); } }
    private Map<String, Object> settings() { return settingService.runtimeValues(SettingGroup.OAUTH); }
    private void requireEnabled(Provider provider, Map<String, Object> settings) { if (!Boolean.parseBoolean(String.valueOf(settings.get(provider.enabledField)))) throw invalid(); }
    private String callbackUrl(Provider provider, Map<String, Object> settings) { String base = text(settings, "callbackBaseUrl"); if (base.isBlank()) throw invalid(); return base.replaceAll("/+$", "") + "/api/auth/oauth/" + provider.value + "/callback"; }
    private String nextUsername(Provider provider, String subject) {
        String base = "oauth_" + provider.value + "_" + Integer.toUnsignedString(subject.hashCode(), 36);
        for (int suffix = 0; ; suffix++) {
            String value = trim(base + (suffix == 0 ? "" : "_" + suffix), 64, "oauth");
            if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, value)) == 0) return value;
        }
    }
    private static String required(JsonNode node, String field) { String value = node.path(field).asText(); if (value.isBlank()) throw invalid(); return value; }
    private static String text(Map<String, Object> values, String key) { Object value = values.get(key); return value == null ? "" : String.valueOf(value).trim(); }
    private static String form(Map<String, String> values) { return values.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).collect(java.util.stream.Collectors.joining("&")); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String trim(String value, int limit, String fallback) { if (value == null || value.isBlank()) return fallback; return value.length() <= limit ? value : value.substring(0, limit); }
    private static BusinessException invalid() { return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST); }

    public record Authorization(String provider, String authorizationUrl) { }
    public record Result(String status, LoginResponse login) { }
    private record PendingState(Provider provider, Instant expiresAt) { }
    private record Identity(String subject, String displayName, String avatarUrl) { }
    private enum Provider {
        GITHUB("github", "githubEnabled", "githubClientId"), WECHAT("wechat", "wechatEnabled", "wechatAppId"), ALIPAY("alipay", "alipayEnabled", "alipayAppId");
        private final String value; private final String enabledField; private final String appIdField;
        Provider(String value, String enabledField, String appIdField) { this.value = value; this.enabledField = enabledField; this.appIdField = appIdField; }
        static Provider parse(String value) { for (Provider provider : values()) if (provider.value.equalsIgnoreCase(value)) return provider; throw invalid(); }
    }
}
