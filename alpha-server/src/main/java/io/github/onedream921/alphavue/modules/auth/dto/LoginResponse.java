package io.github.onedream921.alphavue.modules.auth.dto;

/** The opaque Bearer token and its absolute-session expiry. */
public record LoginResponse(String token, String tokenType, long expiresIn) {
}
