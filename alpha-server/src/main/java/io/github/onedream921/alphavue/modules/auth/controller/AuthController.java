package io.github.onedream921.alphavue.modules.auth.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.auth.dto.LoginRequest;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.github.onedream921.alphavue.modules.auth.dto.ProfileRequests;
import io.github.onedream921.alphavue.modules.auth.service.AuthService;
import io.github.onedream921.alphavue.modules.auth.service.CaptchaService;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.vo.RouteVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 认证接口
 */
@Tag(name = "认证")
@ApiSupport(order = 10, author = "Alpha Vue")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 使用账号密码登录并返回 Bearer Token
     */
    @Operation(summary = "账号登录", description = "校验验证码、账号和密码后签发登录令牌。")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, clientIp(servletRequest)), traceId(servletRequest));
    }

    /**
     * 获取登录验证码挑战
     */
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.CaptchaResponse> captcha(HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.captcha(), traceId(servletRequest));
    }

    /**
     * 退出当前登录会话
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    @OperationLog(module = "Authentication", operation = "Logout", type = BusinessType.LOGOUT)
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        cn.dev33.satoken.stp.StpUtil.logout();
        return ApiResponse.success(null, traceId(servletRequest));
    }

    /**
     * 查询当前登录用户资料
     */
    @Operation(summary = "查询个人资料")
    @GetMapping("/profile")
    @OperationLog(module = "Authentication", operation = "Read profile")
    public ApiResponse<AuthService.Profile> profile(HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.profile(), traceId(servletRequest));
    }

    /**
     * 查询当前登录用户可见路由
     */
    @Operation(summary = "查询可见路由")
    @GetMapping("/routes")
    @OperationLog(module = "Authentication", operation = "Read routes")
    public ApiResponse<List<RouteVo>> routes(HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.routes(), traceId(servletRequest));
    }

    /**
     * 更新当前登录用户资料
     */
    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    @OperationLog(module = "Authentication", operation = "Update profile", type = BusinessType.UPDATE)
    public ApiResponse<AuthService.Profile> updateProfile(@Valid @RequestBody ProfileRequests.Update request,
                                                           HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.updateProfile(request), traceId(servletRequest));
    }

    /**
     * 上传并更新当前用户头像
     */
    @Operation(summary = "上传个人头像")
    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(module = "Authentication", operation = "Upload avatar", type = BusinessType.UPDATE)
    public ApiResponse<AuthService.Profile> uploadAvatar(@RequestPart("file") MultipartFile file,
                                                          HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.uploadAvatar(file), traceId(servletRequest));
    }

    /**
     * 修改当前登录用户密码并退出会话
     */
    @Operation(summary = "修改个人密码")
    @PutMapping("/password")
    @OperationLog(module = "Authentication", operation = "Change password", type = BusinessType.UPDATE)
    public ApiResponse<Void> changePassword(@Valid @RequestBody ProfileRequests.ChangePassword request,
                                            HttpServletRequest servletRequest) {
        authService.changePassword(request);
        return ApiResponse.success(null, traceId(servletRequest));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
