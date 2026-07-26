package io.github.onedream921.alphavue.modules.auth.dto;

/**
 * 登录响应
 */
public record LoginResponse(String token, String tokenType, long expiresIn) {
}
