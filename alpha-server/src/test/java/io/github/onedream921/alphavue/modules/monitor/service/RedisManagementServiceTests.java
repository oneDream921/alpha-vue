package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.github.onedream921.alphavue.modules.monitor.config.RedisDisplayPolicyRegistry;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyMetadataVo;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisManagementServiceTests {

    @Test
    void showsAllValuesWhenAdministratorDisablesMasking() {
        RedisManagementProperties properties = new RedisManagementProperties();
        properties.setMaskValues(false);
        RedisKeyspace keyspace = new RedisKeyspace() {
            @Override public RedisScanResult scan(String prefix, String keyword, String cursor, int count) {
                return new RedisScanResult(List.of(
                        new RedisKeyMetadata("cache:welcome", "string", 1L, 1L, "hello", false),
                        new RedisKeyMetadata("system:dict:dict-test.status", "string", 1L, 1L, "[]", false),
                        new RedisKeyMetadata("satoken:token", "string", 1L, 1L, "secret", false)), "0");
            }
            @Override public RedisKeyMetadata metadata(String key) { return null; }
            @Override public boolean delete(String key) { return false; }
            @Override public RedisOverview overview() { return new RedisOverview(null, null, null, null, Map.of()); }
        };
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.CACHE)).thenReturn(Map.of(
                "redisCaptchaDisplay", "plain", "redisLoginFailureDisplay", "plain", "redisSessionDisplay", "plain",
                "redisDictionaryDisplay", "plain", "redisBusinessDisplay", "plain"));
        RedisManagementService service = new RedisManagementService(
                keyspace, properties, new RedisDisplayPolicyRegistry(), settings);

        List<RedisKeyMetadataVo> records = service.page(new io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery("", "0", 10, null)).records();
        assertThat(records).extracting(RedisKeyMetadataVo::value).containsExactly("hello", "[]", "secret");
        assertThat(records).extracting(RedisKeyMetadataVo::displayLevel)
                .containsExactly("PLAIN", "PLAIN", "PLAIN");
        assertThat(records).extracting(RedisKeyMetadataVo::category)
                .containsExactly("业务/缓存数据", "数据字典缓存", "Sa-Token 会话");
    }

    @Test
    void masksSensitiveNamespacesWhenMaskingIsEnabled() {
        RedisManagementProperties properties = new RedisManagementProperties();
        properties.setMaskValues(true);
        RedisKeyspace keyspace = new RedisKeyspace() {
            @Override public RedisScanResult scan(String prefix, String keyword, String cursor, int count) {
                return new RedisScanResult(List.of(
                        new RedisKeyMetadata("satoken:token", "string", 1L, 1L, "secret", false),
                        new RedisKeyMetadata("unknown:token", "string", 1L, 1L, "secret", false)), "0");
            }
            @Override public RedisKeyMetadata metadata(String key) { return null; }
            @Override public boolean delete(String key) { return false; }
            @Override public RedisOverview overview() { return new RedisOverview(null, null, null, null, Map.of()); }
        };

        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.CACHE)).thenReturn(Map.of(
                "redisCaptchaDisplay", "hidden", "redisLoginFailureDisplay", "hidden", "redisSessionDisplay", "hidden",
                "redisDictionaryDisplay", "masked", "redisBusinessDisplay", "masked"));
        RedisManagementService service = new RedisManagementService(
                keyspace, properties, new RedisDisplayPolicyRegistry(), settings);

        List<RedisKeyMetadataVo> records = service.page(
                new io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery("", "0", 10, null)).records();

        assertThat(records).extracting(RedisKeyMetadataVo::value).containsExactly("[masked]", "[masked]");
        assertThat(records).extracting(RedisKeyMetadataVo::displayLevel).containsExactly("HIDDEN", "MASKED");
    }
}
