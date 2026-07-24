package io.github.onedream921.alphavue.common.exception;

/**
 * Signals a known client-facing failure that can be returned without exposing
 * internal details.
 */
public final class BusinessException extends RuntimeException {

    private final int code;
    private final PublicErrorMessage publicMessage;

    public BusinessException(int code, PublicErrorMessage publicMessage) {
        super(publicMessage.value());
        this.code = code;
        this.publicMessage = publicMessage;
    }

    public int code() {
        return code;
    }

    public PublicErrorMessage publicMessage() {
        return publicMessage;
    }
}
