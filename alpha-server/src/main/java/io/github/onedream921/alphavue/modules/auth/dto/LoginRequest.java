package io.github.onedream921.alphavue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 登录请求
 */
public record LoginRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password,
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,31}") String clientId,
        @Size(max = 64) String deviceId,
        @Size(max = 64) String deviceName,
        Boolean rememberMe,
        @Size(max = 64) String captchaId,
        @Size(max = 16) String captcha) {
}
