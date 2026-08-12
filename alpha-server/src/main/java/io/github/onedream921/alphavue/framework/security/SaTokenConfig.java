package io.github.onedream921.alphavue.framework.security;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import io.github.onedream921.alphavue.modules.auth.service.AuthService;
import io.github.onedream921.alphavue.modules.auth.service.CaptchaService;
import io.github.onedream921.alphavue.modules.file.config.FileStorageProperties;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import io.github.onedream921.alphavue.framework.redis.RedissonCoreAdapter;
import io.github.onedream921.alphavue.framework.redis.RedisPhysicalKey;
import io.github.onedream921.alphavue.framework.redis.RedissonSaTokenDao;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Map;

/**
 * Sa-Token 安全配置
 */
@Configuration
@EnableAsync
public class SaTokenConfig implements WebMvcConfigurer {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);
    private final SysUserMapper userMapper;
    private final FileStorageProperties fileStorageProperties;

    public SaTokenConfig(SysUserMapper userMapper, FileStorageProperties fileStorageProperties) {
        this.userMapper = userMapper;
        this.fileStorageProperties = fileStorageProperties;
    }

    /**
     * Spring Boot 4 下显式注册 Sa-Token Jakarta Servlet 上下文过滤器
     */
    @Bean
    FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> saTokenContextFilter() {
        FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> registration =
                new FilterRegistrationBean<>(new SaTokenContextFilterForJakartaServlet());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 在 Actuator 独立处理链中保护非健康端点
     */
    @Bean
    FilterRegistrationBean<ActuatorAccessFilter> actuatorAccessFilter() {
        FilterRegistrationBean<ActuatorAccessFilter> registration =
                new FilterRegistrationBean<>(new ActuatorAccessFilter(userMapper));
        registration.addUrlPatterns("/actuator", "/actuator/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        return registration;
    }

    @Bean
    FilterRegistrationBean<XssSanitizingFilter> xssSanitizingFilter(SystemSettingService settingService) {
        FilterRegistrationBean<XssSanitizingFilter> registration =
                new FilterRegistrationBean<>(new XssSanitizingFilter(settingService, new com.fasterxml.jackson.databind.ObjectMapper()));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    /**
     * 注册 Sa-Token 登录校验拦截器和公开资源放行规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    StpUtil.checkLogin();
                    requireActiveAccount();
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/captcha",
                        "/api/auth/captcha/slider/**",
                        "/api/auth/oauth/**",
                        "/api/system/settings/public",
                        "/api/wechat/official-account/callback",
                        "/api/auth/test-token",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.ico",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/**/*.svg",
                        "/webjars/**")
                .excludePathPatterns(fileStorageProperties.localPublicPathPattern(), "/api/files/*/content");
    }

    /**
     * 账号被停用或软删除后，立即使已签发的会话失效
     */
    private void requireActiveAccount() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return;
        }
        long userId = Long.parseLong(loginId.toString());
        if (userMapper.countActiveById(userId) == 0) {
            StpUtil.logout();
            StpUtil.checkLogin();
        }
    }

    /**
     * 生产会话使用 Redis 共享；测试环境使用内存 DAO，避免依赖 Docker
     */
    @Bean("saTokenDao")
    @Profile("!test")
    SaTokenDao redisSaTokenDao(RedissonClient redissonClient,
                               @org.springframework.beans.factory.annotation.Qualifier("redissonSaTokenCodec") org.redisson.client.codec.Codec codec) {
        return new RedissonSaTokenDao(redissonClient, codec);
    }

    /**
     * 测试环境使用进程内会话 DAO，保证测试 HTTP 请求间可复用 Token
     */
    @Bean("saTokenDao")
    @Profile("test")
    SaTokenDao inMemorySaTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }

    /**
     * 显式绑定测试 DAO 到 Sa-Token 全局管理器，保证嵌入式 HTTP 请求共享会话
     */
    @Bean
    @Profile("test")
    SmartInitializingSingleton bindTestSaTokenDao(SaTokenDao saTokenDao) {
        return () -> SaManager.setSaTokenDao(saTokenDao);
    }

    /**
     * 生产环境使用 Redis 保存登录失败次数
     */
    @Bean
    @Profile("!test")
    AuthService.LoginFailureStore redisLoginFailureStore(RedissonCoreAdapter redis) {
        return new RedisLoginFailureStore(redis);
    }

    /**
     * 测试环境使用内存保存登录失败次数
     */
    @Bean
    @Profile("test")
    AuthService.LoginFailureStore inMemoryLoginFailureStore() {
        return new InMemoryLoginFailureStore();
    }

    /**
     * 生产环境使用 Redis 保存验证码
     */
    @Bean
    @Profile("!test")
    CaptchaService.CaptchaStore redisCaptchaStore(RedissonCoreAdapter redis) {
        return new CaptchaService.CaptchaStore() {
            /**
             * 保存验证码到 Redis
             */
            @Override
            public void put(String id, String code, Duration ttl) {
                redis.putString(RedisPhysicalKey.forIdentifier("auth", "captcha", id), code, ttl);
            }

            /**
             * 从 Redis 读取并删除验证码
             */
            @Override
            public String consume(String id) {
                return redis.getAndDeleteString(RedisPhysicalKey.forIdentifier("auth", "captcha", id));
            }
        };
    }

    /**
     * 测试环境使用内存保存验证码
     */
    @Bean
    @Profile("test")
    CaptchaService.CaptchaStore inMemoryCaptchaStore() {
        return new CaptchaService.CaptchaStore() {
            private final Map<String, String> values = new java.util.concurrent.ConcurrentHashMap<>();

            /**
             * 保存验证码到内存
             */
            @Override
            public void put(String id, String code, Duration ttl) {
                values.put(id, code);
            }

            /**
             * 从内存读取并删除验证码
             */
            @Override
            public String consume(String id) {
                return values.remove(id);
            }
        };
    }

    private static final class RedisLoginFailureStore implements AuthService.LoginFailureStore {
        private final RedissonCoreAdapter redis;

        private RedisLoginFailureStore(RedissonCoreAdapter redis) {
            this.redis = redis;
        }

        /**
         * 通过 Redis 原子脚本占用一次登录尝试
         */
        @Override
        public boolean reserveAttempt(String username, String ipAddress, int limit, Duration window) {
            return redis.reserveAttempt(RedisPhysicalKey.forIdentifier("auth", "login-failure",
                    username + "\u0000" + ipAddress), limit, window);
        }

        /**
         * 清除 Redis 中的登录失败记录
         */
        @Override
        public void clear(String username, String ipAddress) {
            redis.delete(RedisPhysicalKey.forIdentifier("auth", "login-failure",
                    username + "\u0000" + ipAddress));
        }
    }

    private static final class InMemoryLoginFailureStore implements AuthService.LoginFailureStore {
        private final Map<String, FailureWindow> attempts = new java.util.HashMap<>();

        /**
         * 在内存窗口中占用一次登录尝试
         */
        @Override
        public synchronized boolean reserveAttempt(String username, String ipAddress, int limit, Duration window) {
            String key = username + "\u0000" + ipAddress;
            long now = System.nanoTime();
            FailureWindow current = attempts.get(key);
            if (current == null || current.expiresAtNanos() <= now) {
                attempts.put(key, new FailureWindow(1, now + window.toNanos()));
                return true;
            }
            if (current.attempts() >= limit) {
                return false;
            }
            attempts.put(key, new FailureWindow(current.attempts() + 1, current.expiresAtNanos()));
            return true;
        }

        /**
         * 清除内存中的登录失败记录
         */
        @Override
        public synchronized void clear(String username, String ipAddress) {
            attempts.remove(username + "\u0000" + ipAddress);
        }

        private record FailureWindow(int attempts, long expiresAtNanos) {
        }
    }

}
