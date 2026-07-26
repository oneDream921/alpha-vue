package io.github.onedream921.alphavue.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTests {

    @Test
    void successUsesOkCodeAndSuppliedTraceId() {
        ApiResponse<String> response = ApiResponse.success("payload", "trace-success");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.traceId()).isEqualTo("trace-success");
    }

    @Test
    void errorPreservesSuppliedTraceId() {
        ApiResponse<Void> response = ApiResponse.error(400, "请求参数错误", "trace-error");

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("请求参数错误");
        assertThat(response.data()).isNull();
        assertThat(response.traceId()).isEqualTo("trace-error");
    }
}
