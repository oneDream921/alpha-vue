package io.github.onedream921.alphavue.modules.auth.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试 Token 接口
 */
@Profile("test")
@Tag(name = "测试认证")
@ApiSupport(order = 99, author = "Alpha Vue")
@RestController
@RequestMapping("/api/auth")
public class TestTokenController extends BaseController {

    private final long userId;
    private final String token;
    private final long timeoutSeconds;

    public TestTokenController(
            @Value("${alpha.test-token.user-id:1}") long userId,
            @Value("${alpha.test-token.value:alpha-test-admin-token}") String token,
            @Value("${alpha.test-token.timeout-seconds:28800}") long timeoutSeconds) {
        this.userId = userId;
        this.token = token;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 签发固定 Bearer Token，并绑定到配置的测试用户
     */
    @Operation(summary = "签发测试令牌", hidden = true)
    @PostMapping("/test-token")
    public ApiResponse<LoginResponse> issue(HttpServletRequest request) {
        StpUtil.login(userId, SaLoginParameter.create()
                .setToken(token)
                .setTimeout(timeoutSeconds)
                .setIsWriteHeader(false));
        return success(new LoginResponse(token, "Bearer", timeoutSeconds), request);
    }
}
