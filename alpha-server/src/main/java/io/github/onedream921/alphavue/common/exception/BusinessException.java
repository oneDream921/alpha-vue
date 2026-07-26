package io.github.onedream921.alphavue.common.exception;

/**
 * 业务异常
 */
public final class BusinessException extends RuntimeException {

    private final int code;
    private final PublicErrorMessage publicMessage;

    /**
     * 创建带公共错误消息的业务异常
     */
    public BusinessException(int code, PublicErrorMessage publicMessage) {
        super(publicMessage.value());
        this.code = code;
        this.publicMessage = publicMessage;
    }

    /**
     * 返回可映射为 HTTP 状态的业务错误码
     */
    public int code() {
        return code;
    }

    /**
     * 返回允许暴露给客户端的错误消息
     */
    public PublicErrorMessage publicMessage() {
        return publicMessage;
    }
}
