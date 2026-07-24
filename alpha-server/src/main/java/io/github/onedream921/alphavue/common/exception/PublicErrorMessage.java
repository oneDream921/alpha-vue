package io.github.onedream921.alphavue.common.exception;

/** Fixed, client-safe messages that may be included in API error responses. */
public enum PublicErrorMessage {
    INVALID_REQUEST("Invalid request"),
    VALIDATION_FAILED("Validation failed"),
    INTERNAL_SERVER_ERROR("Internal server error");

    private final String value;

    PublicErrorMessage(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
