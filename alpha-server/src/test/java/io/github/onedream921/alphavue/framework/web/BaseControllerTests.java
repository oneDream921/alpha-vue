package io.github.onedream921.alphavue.framework.web;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class BaseControllerTests {

    private final TestController controller = new TestController();

    @Test
    void successUsesRequestTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-controller");

        ApiResponse<String> response = controller.ok("payload", request);

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.traceId()).isEqualTo("trace-controller");
    }

    @Test
    void voidSuccessKeepsResponseEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-empty");

        ApiResponse<Void> response = controller.empty(request);

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data()).isNull();
        assertThat(response.traceId()).isEqualTo("trace-empty");
    }

    private static final class TestController extends BaseController {

        private ApiResponse<String> ok(String data, MockHttpServletRequest request) {
            return success(data, request);
        }

        private ApiResponse<Void> empty(MockHttpServletRequest request) {
            return success(request);
        }
    }
}
