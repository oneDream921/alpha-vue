package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.github.onedream921.alphavue.modules.monitor.config.RedisDisplayPolicyRegistry;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyMetadataVo;
import io.github.onedream921.alphavue.modules.system.service.ConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisManagementServiceTests {

    @Test
    void canShowNonSensitiveValuesWhenConfiguredButNeverShowsSessionTokens() {
        RedisManagementProperties properties = new RedisManagementProperties();
        properties.setMaskValues(false);
        RedisManagementService service = new RedisManagementService(new RedisKeyspace() {
            @Override public RedisScanResult scan(String prefix, String keyword, String cursor, int count) {
                return new RedisScanResult(List.of(
                        new RedisKeyMetadata("cache:welcome", "string", 1L, 1L, "hello", false),
                        new RedisKeyMetadata("system:dict:dict-test.status", "string", 1L, 1L, "[]", false),
                        new RedisKeyMetadata("satoken:token", "string", 1L, 1L, "secret", false)), "0");
            }
            @Override public RedisKeyMetadata metadata(String key) { return null; }
            @Override public boolean delete(String key) { return false; }
            @Override public RedisOverview overview() { return new RedisOverview(null, null, null, null, Map.of()); }
        }, properties);

        List<RedisKeyMetadataVo> records = service.page(new io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery("", "0", 10, null)).records();
        assertThat(records).extracting(RedisKeyMetadataVo::value).containsExactly("[masked]", "[masked]", "[masked]");
        assertThat(records).extracting(RedisKeyMetadataVo::displayLevel)
                .containsExactly("MASKED", "MASKED", "HIDDEN");
        assertThat(records).extracting(RedisKeyMetadataVo::category)
                .containsExactly("业务/缓存数据", "数据字典缓存", "Sa-Token 会话");
    }

    @Test
    void allowsRegisteredSensitiveNamespacesToUseTheSameDisplayLevels() {
        RedisManagementProperties properties = new RedisManagementProperties();
        properties.setMaskValues(false);
        ConfigService configService = mock(ConfigService.class);
        when(configService.value("cache.display.session")).thenReturn("PLAIN");
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

        RedisManagementService service = new RedisManagementService(
                keyspace, properties, new RedisDisplayPolicyRegistry(), configService);

        List<RedisKeyMetadataVo> records = service.page(
                new io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery("", "0", 10, null)).records();

        assertThat(records).extracting(RedisKeyMetadataVo::value).containsExactly("secret", "[masked]");
        assertThat(records).extracting(RedisKeyMetadataVo::displayLevel).containsExactly("PLAIN", "HIDDEN");
    }
}
