package io.github.onedream921.alphavue.framework.security;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 保护非健康 Actuator 端点
 */
public class ActuatorAccessFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_ROOT = "/actuator";
    private static final String HEALTH_PREFIX = "/actuator/health";

    private final SysUserMapper userMapper;

    public ActuatorAccessFilter(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 健康探针公开，其余 Actuator 端点必须使用 Bearer 会话
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isActuatorPath(path)
                && !isPublicHealthPath(path)
                && !hasValidBearerToken(request)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasValidBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        Object loginId = token.isEmpty() ? null : StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            return false;
        }
        return userMapper.countActiveById(Long.parseLong(loginId.toString())) > 0;
    }

    private boolean isActuatorPath(String path) {
        return ACTUATOR_ROOT.equals(path) || path.startsWith(ACTUATOR_ROOT + "/");
    }

    private boolean isPublicHealthPath(String path) {
        return HEALTH_PREFIX.equals(path) || path.startsWith(HEALTH_PREFIX + "/");
    }
}
