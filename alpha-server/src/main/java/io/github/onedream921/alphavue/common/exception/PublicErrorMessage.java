package io.github.onedream921.alphavue.common.exception;

/**
 * 公共错误消息枚举
 */
public enum PublicErrorMessage {
    INVALID_REQUEST("请求参数错误"),
    VALIDATION_FAILED("参数校验失败"),
    INVALID_CREDENTIALS("用户名或密码错误"),
    CURRENT_PASSWORD_INCORRECT("旧密码错误"),
    PASSWORD_MUST_DIFFER("新密码不能与旧密码相同"),
    LOGIN_TEMPORARILY_LOCKED("登录失败次数过多，请稍后再试"),
    UNAUTHORIZED("请先登录"),
    FORBIDDEN("没有操作权限"),
    INTERNAL_SERVER_ERROR("服务器内部错误");

    private final String value;

    PublicErrorMessage(String value) {
        this.value = value;
    }

    /**
     * 返回客户端可见的固定消息文本
     */
    public String value() {
        return value;
    }
}
