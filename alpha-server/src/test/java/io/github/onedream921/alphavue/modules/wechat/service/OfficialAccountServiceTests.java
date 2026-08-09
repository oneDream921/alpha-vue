package io.github.onedream921.alphavue.modules.wechat.service;

import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfficialAccountServiceTests {
    @Test
    void verifiesWechatSignature() {
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.OFFICIAL_ACCOUNT)).thenReturn(Map.of("token", "token"));
        OfficialAccountService service = new OfficialAccountService(settings);

        assertThat(service.verifies("8779cd22a93aad0cb09babdc953a6d114bbf1c53", "123", "456")).isTrue();
        assertThat(service.verifies("invalid", "123", "456")).isFalse();
    }
}
