package io.github.onedream921.alphavue.common.exception;

/**
 * Signals a known client-facing failure that can be returned without exposing
 * internal details.
 */
public final class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
