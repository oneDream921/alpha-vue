package io.github.onedream921.alphavue.modules.auth.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.framework.web.ClientAddressResolver;
import io.github.onedream921.alphavue.modules.auth.dto.LoginRequest;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.github.onedream921.alphavue.modules.auth.dto.ProfileRequests;
import io.github.onedream921.alphavue.modules.auth.service.AuthService;
import io.github.onedream921.alphavue.modules.auth.service.CaptchaService;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.vo.RouteVo;
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
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final AuthService authService;
    private final ClientAddressResolver clientAddressResolver;

    public AuthController(AuthService authService, ClientAddressResolver clientAddressResolver) {
        this.authService = authService;
        this.clientAddressResolver = clientAddressResolver;
    }

    /**
     * 使用账号密码登录并返回 Bearer Token
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return success(authService.login(request, clientAddressResolver.resolve(servletRequest), servletRequest.getHeader("User-Agent"),
                traceId(servletRequest)), servletRequest);
    }

    /**
     * 获取登录验证码挑战
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.CaptchaResponse> captcha(HttpServletRequest servletRequest) {
        return success(authService.captcha(), servletRequest);
    }

    /**
     * 退出当前登录会话
     */
    @PostMapping("/logout")
    @OperationLog(module = "Authentication", operation = "Logout", type = BusinessType.LOGOUT)
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        cn.dev33.satoken.stp.StpUtil.logout();
        return success(servletRequest);
    }

    /**
     * 查询当前登录用户资料
     */
    @GetMapping("/profile")
    @OperationLog(module = "Authentication", operation = "Read profile")
    public ApiResponse<AuthService.Profile> profile(HttpServletRequest servletRequest) {
        return success(authService.profile(), servletRequest);
    }

    /**
     * 查询当前登录用户可见路由
     */
    @GetMapping("/routes")
    @OperationLog(module = "Authentication", operation = "Read routes")
    public ApiResponse<List<RouteVo>> routes(HttpServletRequest servletRequest) {
        return success(authService.routes(), servletRequest);
    }

    /**
     * 更新当前登录用户资料
     */
    @PutMapping("/profile")
    @OperationLog(module = "Authentication", operation = "Update profile", type = BusinessType.UPDATE)
    public ApiResponse<AuthService.Profile> updateProfile(@Valid @RequestBody ProfileRequests.Update request,
                                                           HttpServletRequest servletRequest) {
        return success(authService.updateProfile(request), servletRequest);
    }

    /**
     * 上传并更新当前用户头像
     */
    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(module = "Authentication", operation = "Upload avatar", type = BusinessType.UPDATE)
    public ApiResponse<AuthService.Profile> uploadAvatar(@RequestPart("file") MultipartFile file,
                                                          HttpServletRequest servletRequest) {
        return success(authService.uploadAvatar(file), servletRequest);
    }

    /**
     * 修改当前登录用户密码并退出会话
     */
    @PutMapping("/password")
    @OperationLog(module = "Authentication", operation = "Change password", type = BusinessType.UPDATE)
    public ApiResponse<Void> changePassword(@Valid @RequestBody ProfileRequests.ChangePassword request,
                                            HttpServletRequest servletRequest) {
        authService.changePassword(request);
        return success(servletRequest);
    }
}
