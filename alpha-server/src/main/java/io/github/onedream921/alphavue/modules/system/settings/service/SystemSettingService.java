package io.github.onedream921.alphavue.modules.system.settings.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.config.SettingCipher;
import io.github.onedream921.alphavue.modules.system.settings.dto.SystemSettingRequests;
import io.github.onedream921.alphavue.modules.system.settings.entity.SysSystemSetting;
import io.github.onedream921.alphavue.modules.system.settings.mapper.SysSystemSettingMapper;
import io.github.onedream921.alphavue.modules.system.settings.vo.SystemSettingVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.security.KeyPairGenerator;
import java.util.Base64;

@Service
public class SystemSettingService {
    public record RsaKeyPair(String publicKey, String privateKey) { }
    public record FileStorageCredentials(String accessKey, String secretKey) { }
    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() { };
    private final SysSystemSettingMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SettingCipher cipher;
    private static final Map<String, String> REDIS_DISPLAY_DEFAULTS = Map.of(
            "redisCaptchaDisplay", "hidden",
            "redisLoginFailureDisplay", "hidden",
            "redisSessionDisplay", "hidden",
            "redisDictionaryDisplay", "masked",
            "redisBusinessDisplay", "masked");

    public SystemSettingService(SysSystemSettingMapper mapper, SettingCipher cipher) {
        this.mapper = mapper;
        this.cipher = cipher;
    }

    public SystemSettingVo get(SettingGroup group) {
        SysSystemSetting entity = find(group);
        Map<String, Object> values = entity == null ? new LinkedHashMap<>() : read(entity.getValuesJson());
        if (group == SettingGroup.CACHE) values = cacheDisplayValues(values);
        Map<String, Boolean> secrets = new LinkedHashMap<>();
        if (entity != null && entity.getSecretsCiphertext() != null && !entity.getSecretsCiphertext().isBlank()) {
            read(cipher.decrypt(entity.getSecretsCiphertext())).keySet().forEach(key -> secrets.put(key, true));
        }
        return new SystemSettingVo(group.name().toLowerCase(java.util.Locale.ROOT), Map.copyOf(values), Map.copyOf(secrets),
                group == SettingGroup.FILE || group == SettingGroup.OAUTH || group == SettingGroup.PAYMENT || group == SettingGroup.OFFICIAL_ACCOUNT);
    }

    @Transactional
    public SystemSettingVo save(SettingGroup group, SystemSettingRequests.Save request) {
        try { group.validate(request.values()); } catch (IllegalArgumentException exception) { throw invalid(); }
        SysSystemSetting entity = find(group);
        if (entity == null) { entity = new SysSystemSetting(); entity.setSettingGroup(group.name()); entity.setKeyVersion(1); }
        Map<String, Object> values = entity.getValuesJson() == null ? new LinkedHashMap<>() : read(entity.getValuesJson());
        Map<String, Object> secrets = entity.getSecretsCiphertext() == null ? new LinkedHashMap<>() : read(cipher.decrypt(entity.getSecretsCiphertext()));
        request.values().forEach((key, value) -> {
            if (group.isSecret(key)) {
                if (value instanceof String text && !text.isBlank()) secrets.put(key, text);
            } else values.put(key, value);
        });
        entity.setValuesJson(write(values));
        entity.setSecretsCiphertext(secrets.isEmpty() ? null : cipher.encrypt(write(secrets)));
        if (entity.getId() == null) mapper.insert(entity); else mapper.updateById(entity);
        return get(group);
    }

    /** Safe anonymous bootstrap data only. */
    public Map<String, Object> publicSettings() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("site", get(SettingGroup.SITE).values());
        SystemSettingVo login = get(SettingGroup.LOGIN);
        response.put("login", Map.of("captchaEnabled", login.values().getOrDefault("captchaEnabled", true),
                "captchaType", "numeric",
                "rememberMeEnabled", login.values().getOrDefault("rememberMeEnabled", true)));
        return response;
    }

    /** Internal runtime view. Never expose the returned secret entries through a controller. */
    public Map<String, Object> runtimeValues(SettingGroup group) {
        SysSystemSetting entity = find(group);
        if (entity == null) return Map.of();
        Map<String, Object> values = new LinkedHashMap<>(read(entity.getValuesJson()));
        if (group == SettingGroup.CACHE) values = cacheDisplayValues(values);
        if (entity.getSecretsCiphertext() != null && !entity.getSecretsCiphertext().isBlank()) {
            values.putAll(read(cipher.decrypt(entity.getSecretsCiphertext())));
        }
        return Map.copyOf(values);
    }

    private static Map<String, Object> cacheDisplayValues(Map<String, Object> source) {
        Map<String, Object> values = new LinkedHashMap<>();
        boolean hasCategorySettings = REDIS_DISPLAY_DEFAULTS.keySet().stream().anyMatch(source::containsKey);
        if (hasCategorySettings) {
            REDIS_DISPLAY_DEFAULTS.forEach((key, fallback) -> values.put(key, source.getOrDefault(key, fallback)));
            return values;
        }
        boolean legacyMasking = !(source.get("redisMaskValues") instanceof Boolean enabled) || enabled;
        REDIS_DISPLAY_DEFAULTS.forEach((key, fallback) -> values.put(key, legacyMasking ? fallback : "plain"));
        return values;
    }

    /**
     * Returns only the two file storage credentials for the explicitly authorized
     * settings screen. General settings reads must continue to use {@link #get(SettingGroup)}.
     */
    public FileStorageCredentials fileStorageCredentials() {
        SysSystemSetting entity = find(SettingGroup.FILE);
        if (entity == null || entity.getSecretsCiphertext() == null || entity.getSecretsCiphertext().isBlank()) {
            return new FileStorageCredentials(null, null);
        }
        Map<String, Object> secrets = read(cipher.decrypt(entity.getSecretsCiphertext()));
        return new FileStorageCredentials(textValue(secrets.get("accessKey")), textValue(secrets.get("secretKey")));
    }

    public RsaKeyPair regenerateRsaKeys() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
            return new RsaKeyPair(publicKey, privateKey);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate RSA keys", exception);
        }
    }

    private SysSystemSetting find(SettingGroup group) { return mapper.selectOne(new LambdaQueryWrapper<SysSystemSetting>()
            .eq(SysSystemSetting::getSettingGroup, group.name())); }
    private static String textValue(Object value) { return value instanceof String text ? text : null; }
    private Map<String, Object> read(String source) { try { return objectMapper.readValue(source, MAP); } catch (Exception exception) { throw invalid(); } }
    private String write(Map<String, Object> value) { try { return objectMapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    private static BusinessException invalid() { return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST); }
}
