package io.github.onedream921.alphavue.common.exception;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionReturnsOnlyTheSelectedSafePublicMessage() {
        MockHttpServletRequest request = requestWithTraceId();
        BusinessException exception = new BusinessException(
                400,
                PublicErrorMessage.INVALID_REQUEST);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid request");
        assertThat(response.getBody().traceId()).isEqualTo("trace-123");
    }

    @Test
    void unexpectedExceptionReturnsGenericPublicMessage() {
        MockHttpServletRequest request = requestWithTraceId();

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(
                new IllegalStateException("Database password leaked"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
        assertThat(response.getBody().message()).doesNotContain("Database password");
        assertThat(response.getBody().traceId()).isEqualTo("trace-123");
    }

    private MockHttpServletRequest requestWithTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-123");
        return request;
    }
}
