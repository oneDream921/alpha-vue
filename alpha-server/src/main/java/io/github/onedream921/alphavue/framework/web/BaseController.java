package io.github.onedream921.alphavue.framework.web;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 控制层基类，集中处理统一响应与请求上下文读取。
 */
public abstract class BaseController {

    protected final <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, traceId(request));
    }

    protected final ApiResponse<Void> success(HttpServletRequest request) {
        return ApiResponse.success(null, traceId(request));
    }

    protected final String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        return traceId == null ? null : traceId.toString();
    }

    protected final String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    protected final long loginUserId() {
        return Long.parseLong(StpUtil.getLoginIdAsString());
    }
}
