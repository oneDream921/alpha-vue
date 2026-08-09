package io.github.onedream921.alphavue.modules.auth.service;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.modules.auth.mapper.SysOauthAccountMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OauthServiceTests {
    @Test
    void createsGithubAuthorizationUrlWithOneTimeState() {
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.OAUTH)).thenReturn(Map.of(
                "githubEnabled", true, "githubClientId", "client-id", "callbackBaseUrl", "https://admin.example.com"));
        OauthService service = service(settings);

        OauthService.Authorization authorization = service.begin("github");

        assertThat(authorization.provider()).isEqualTo("github");
        assertThat(authorization.authorizationUrl()).contains("github.com/login/oauth/authorize", "client_id=client-id",
                "redirect_uri=https%3A%2F%2Fadmin.example.com%2Fapi%2Fauth%2Foauth%2Fgithub%2Fcallback", "state=");
    }

    @Test
    void rejectsDisabledProvider() {
        SystemSettingService settings = mock(SystemSettingService.class);
        when(settings.runtimeValues(SettingGroup.OAUTH)).thenReturn(Map.of("githubEnabled", false));

        assertThatThrownBy(() -> service(settings).begin("github")).isInstanceOf(BusinessException.class);
    }

    private static OauthService service(SystemSettingService settings) {
        return new OauthService(settings, mock(SysOauthAccountMapper.class), mock(SysUserMapper.class),
                mock(AuthService.class));
    }
}
