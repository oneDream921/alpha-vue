package io.github.onedream921.alphavue.modules.auth;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.auth.dto.LoginRequest;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Authentication endpoints consumed by the application shell. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, clientIp(servletRequest)), traceId(servletRequest));
    }

    @PostMapping("/logout")
    @OperationLog(module = "Authentication", operation = "Logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        cn.dev33.satoken.stp.StpUtil.logout();
        return ApiResponse.success(null, traceId(servletRequest));
    }

    @GetMapping("/profile")
    @OperationLog(module = "Authentication", operation = "Read profile")
    public ApiResponse<AuthService.Profile> profile(HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.profile(), traceId(servletRequest));
    }

    @GetMapping("/routes")
    @OperationLog(module = "Authentication", operation = "Read routes")
    public ApiResponse<List<Map<String, Object>>> routes(HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.routes(), traceId(servletRequest));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
