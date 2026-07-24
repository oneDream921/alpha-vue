package io.github.onedream921.alphavue.common.api;

/**
 * Immutable response envelope returned by the HTTP API.
 *
 * @param code application or HTTP-compatible status code
 * @param message human-readable result message
 * @param data response payload
 * @param traceId identifier for correlating a request with application logs
 */
public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(200, "ok", data, traceId);
    }

    public static <T> ApiResponse<T> error(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
