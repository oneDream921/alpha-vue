package io.github.onedream921.alphavue.common.api;

/**
 * 统一响应体
 *
 * @param code 业务状态码或兼容的 HTTP 状态码
 * @param message 可读的结果消息
 * @param data 响应数据
 * @param traceId 关联请求与应用日志的标识
 */
public record ApiResponse<T>(int code, String message, T data, String traceId) {

    /**
     * 构建成功响应
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(200, "ok", data, traceId);
    }

    /**
     * 构建错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
