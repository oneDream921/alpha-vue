package io.github.onedream921.alphavue.framework.security;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 将 Authorization Bearer Token 写入当前 Sa-Token 请求上下文
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class BearerTokenFilter extends OncePerRequestFilter {

    /**
     * 仅接受标准 Bearer Token，避免 Cookie 或请求体携带会话凭据
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            if (!token.isEmpty()) {
                StpUtil.setTokenValueToStorage(token);
            }
        }
        filterChain.doFilter(request, response);
    }
}
