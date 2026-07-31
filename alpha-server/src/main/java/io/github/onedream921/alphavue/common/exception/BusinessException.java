package io.github.onedream921.alphavue.common.exception;

/**
 * 业务异常
 */
public final class BusinessException extends RuntimeException {

    private final int code;
    private final PublicErrorMessage publicMessage;
    private final String auditSummary;

    /**
     * 创建带公共错误消息的业务异常
     */
    public BusinessException(int code, PublicErrorMessage publicMessage) {
        this(code, publicMessage, publicMessage.value());
    }

    /**
     * 创建带公共消息和内部审计摘要的业务异常。
     */
    public BusinessException(int code, PublicErrorMessage publicMessage, String auditSummary) {
        super(publicMessage.value());
        this.code = code;
        this.publicMessage = publicMessage;
        this.auditSummary = auditSummary == null || auditSummary.isBlank()
                ? publicMessage.value() : auditSummary;
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

    /**
     * 返回不暴露给客户端的审计摘要。
     */
    public String auditSummary() {
        return auditSummary;
    }
}
