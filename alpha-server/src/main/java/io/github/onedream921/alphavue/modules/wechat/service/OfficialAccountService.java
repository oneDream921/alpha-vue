package io.github.onedream921.alphavue.modules.wechat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/** WeChat official-account signature verification and custom-menu publishing. */
@Service
public class OfficialAccountService {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final SystemSettingService settingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OfficialAccountService(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    public boolean verifies(String signature, String timestamp, String nonce) {
        if (signature == null || timestamp == null || nonce == null) return false;
        String token = value("token");
        if (token.isBlank()) return false;
        String[] values = {token, timestamp, nonce};
        Arrays.sort(values);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(String.join("", values).getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(hex(digest).getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) { return false; }
    }

    public String oauthAuthorizationUrl(String state) {
        String appId = required("appId");
        String redirect = required("oauthCallbackUrl");
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + encode(appId)
                + "&redirect_uri=" + encode(redirect) + "&response_type=code&scope=snsapi_userinfo&state=" + encode(state)
                + "#wechat_redirect";
    }

    public void publishMenu() {
        String menu = required("customMenuJson");
        try { objectMapper.readTree(menu); } catch (Exception exception) { throw invalid(); }
        String accessToken = accessToken();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + encode(accessToken)))
                    .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(menu, StandardCharsets.UTF_8)).build();
            JsonNode result = objectMapper.readTree(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body());
            if (result == null || result.path("errcode").asInt(-1) != 0) throw invalid();
        } catch (BusinessException exception) { throw exception; }
        catch (Exception exception) { throw invalid(); }
    }

    private String accessToken() {
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + encode(required("appId"))
                    + "&secret=" + encode(required("appSecret"));
            JsonNode result = objectMapper.readTree(HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).body());
            String token = result == null ? "" : result.path("access_token").asText();
            if (token.isBlank()) throw invalid();
            return token;
        } catch (BusinessException exception) { throw exception; }
        catch (Exception exception) { throw invalid(); }
    }

    private String value(String key) {
        Object value = settingService.runtimeValues(SettingGroup.OFFICIAL_ACCOUNT).get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
    private String required(String key) { String value = value(key); if (value.isBlank()) throw invalid(); return value; }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String hex(byte[] bytes) { StringBuilder result = new StringBuilder(bytes.length * 2); for (byte value : bytes) result.append(String.format("%02x", value)); return result.toString(); }
    private static BusinessException invalid() { return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST); }
}
