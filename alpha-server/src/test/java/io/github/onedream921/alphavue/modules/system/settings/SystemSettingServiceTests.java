package io.github.onedream921.alphavue.modules.system.settings;

import io.github.onedream921.alphavue.modules.system.settings.config.SettingCipher;
import io.github.onedream921.alphavue.modules.system.settings.dto.SystemSettingRequests;
import io.github.onedream921.alphavue.modules.system.settings.entity.SysSystemSetting;
import io.github.onedream921.alphavue.modules.system.settings.mapper.SysSystemSettingMapper;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemSettingServiceTests {
    private static final String KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void encryptsSecretsAndNeverReturnsTheirPlaintext() {
        SysSystemSettingMapper mapper = mock(SysSystemSettingMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        SystemSettingService service = new SystemSettingService(mapper, new SettingCipher(KEY));
        service.save(SettingGroup.MINI_PROGRAM, new SystemSettingRequests.Save(Map.of("appId", "wx-app", "appSecret", "plain-secret")));
        ArgumentCaptor<SysSystemSetting> captured = ArgumentCaptor.forClass(SysSystemSetting.class);
        verify(mapper).insert(captured.capture());
        assertThat(captured.getValue().getSecretsCiphertext()).doesNotContain("plain-secret");
        when(mapper.selectOne(any())).thenReturn(captured.getValue());
        var response = service.get(SettingGroup.MINI_PROGRAM);
        assertThat(response.values()).containsEntry("appId", "wx-app").doesNotContainKey("appSecret");
        assertThat(response.secretConfigured()).containsEntry("appSecret", true);
    }

    @Test
    void rejectsUnregisteredFields() {
        SystemSettingService service = new SystemSettingService(mock(SysSystemSettingMapper.class), new SettingCipher(KEY));
        assertThatThrownBy(() -> service.save(SettingGroup.LOGIN, new SystemSettingRequests.Save(Map.of("spring.datasource.url", "bad"))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsInvalidRegisteredValuesBeforePersistence() {
        SysSystemSettingMapper mapper = mock(SysSystemSettingMapper.class);
        SystemSettingService service = new SystemSettingService(mapper, new SettingCipher(KEY));

        assertThatThrownBy(() -> service.save(SettingGroup.FILE,
                new SystemSettingRequests.Save(Map.of("provider", "ftp"))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.save(SettingGroup.SITE,
                new SystemSettingRequests.Save(Map.of("watermarkOpacity", 0.9))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.save(SettingGroup.OAUTH,
                new SystemSettingRequests.Save(Map.of("callbackBaseUrl", "javascript:alert(1)"))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.save(SettingGroup.OFFICIAL_ACCOUNT,
                new SystemSettingRequests.Save(Map.of("customMenuJson", "not-json"))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.save(SettingGroup.OFFICIAL_ACCOUNT,
                new SystemSettingRequests.Save(Map.of("customMenuJson", "{\"button\":}"))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.save(SettingGroup.LOGIN,
                new SystemSettingRequests.Save(Map.of("maxRetry", 21))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.save(SettingGroup.LOGIN,
                new SystemSettingRequests.Save(Map.of("lockMinutes", 1.5))))
                .isInstanceOf(RuntimeException.class);
        verify(mapper, never()).insert(any(SysSystemSetting.class));
        verify(mapper, never()).updateById(any(SysSystemSetting.class));
    }

    @Test
    void regeneratesRsaKeyPairReturnsOneTimePairWithoutPersistingUntilExplicitSave() {
        SysSystemSettingMapper mapper = mock(SysSystemSettingMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        var service = new SystemSettingService(mapper, new SettingCipher(KEY));
        var pair = service.regenerateRsaKeys();
        verify(mapper, never()).insert(any(SysSystemSetting.class));
        verify(mapper, never()).updateById(any(SysSystemSetting.class));
        assertThat(pair.publicKey()).isNotBlank();
        assertThat(pair.privateKey()).isNotBlank();

        service.save(SettingGroup.SECURITY, new SystemSettingRequests.Save(Map.of(
                "rsaPublicKey", pair.publicKey(), "rsaPrivateKey", pair.privateKey())));
        ArgumentCaptor<SysSystemSetting> captured = ArgumentCaptor.forClass(SysSystemSetting.class);
        verify(mapper).insert(captured.capture());
        assertThat(captured.getValue().getValuesJson()).contains("rsaPublicKey").doesNotContain("rsaPrivateKey");
        assertThat(captured.getValue().getSecretsCiphertext()).isNotBlank();
        when(mapper.selectOne(any())).thenReturn(captured.getValue());
        assertThat(service.get(SettingGroup.SECURITY).values())
                .doesNotContainKey("rsaPrivateKey");
    }

    @Test
    void revealsOnlyFileStorageCredentialsThroughDedicatedServiceMethod() {
        SysSystemSettingMapper mapper = mock(SysSystemSettingMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        SystemSettingService service = new SystemSettingService(mapper, new SettingCipher(KEY));
        service.save(SettingGroup.FILE, new SystemSettingRequests.Save(Map.of(
                "provider", "minio", "accessKey", "alpha-access", "secretKey", "alpha-secret")));
        ArgumentCaptor<SysSystemSetting> captured = ArgumentCaptor.forClass(SysSystemSetting.class);
        verify(mapper).insert(captured.capture());
        when(mapper.selectOne(any())).thenReturn(captured.getValue());

        assertThat(service.get(SettingGroup.FILE).values())
                .doesNotContainKeys("accessKey", "secretKey");
        assertThat(service.fileStorageCredentials())
                .extracting(SystemSettingService.FileStorageCredentials::accessKey,
                        SystemSettingService.FileStorageCredentials::secretKey)
                .containsExactly("alpha-access", "alpha-secret");
    }
}
