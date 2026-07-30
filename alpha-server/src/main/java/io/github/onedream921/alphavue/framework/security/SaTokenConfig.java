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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 安全配置
 */
@Configuration
@EnableAsync
public class SaTokenConfig implements WebMvcConfigurer {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final int MAX_TOKEN_SEARCH_PAGE_SIZE = 100;
    private static final int MAX_TOKEN_SEARCH_KEYS = 1_000;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final DefaultRedisScript<Long> RESERVE_LOGIN_ATTEMPT = new DefaultRedisScript<>("""
            local attempts = redis.call('GET', KEYS[1])
            if attempts and tonumber(attempts) >= tonumber(ARGV[1]) then
                return 0
            end
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 1
            """, Long.class);
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
    SaTokenDao redisSaTokenDao(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.afterPropertiesSet();
        return new RedisBackedSaTokenDao(template);
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
    AuthService.LoginFailureStore redisLoginFailureStore(StringRedisTemplate redisTemplate) {
        return new RedisLoginFailureStore(redisTemplate);
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
    CaptchaService.CaptchaStore redisCaptchaStore(StringRedisTemplate redisTemplate) {
        return new CaptchaService.CaptchaStore() {
            /**
             * 保存验证码到 Redis
             */
            @Override
            public void put(String id, String code, Duration ttl) {
                redisTemplate.opsForValue().set("auth:captcha:" + id, code, ttl);
            }

            /**
             * 从 Redis 读取并删除验证码
             */
            @Override
            public String consume(String id) {
                return redisTemplate.opsForValue().getAndDelete("auth:captcha:" + id);
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
        private final StringRedisTemplate redisTemplate;

        private RedisLoginFailureStore(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * 通过 Redis 原子脚本占用一次登录尝试
         */
        @Override
        public boolean reserveAttempt(String username, String ipAddress) {
            Long reserved = redisTemplate.execute(
                    RESERVE_LOGIN_ATTEMPT,
                    List.of(key(username, ipAddress)),
                    Integer.toString(MAX_LOGIN_FAILURES),
                    Long.toString(LOGIN_FAILURE_WINDOW.toMillis()));
            return Long.valueOf(1L).equals(reserved);
        }

        /**
         * 清除 Redis 中的登录失败记录
         */
        @Override
        public void clear(String username, String ipAddress) {
            redisTemplate.delete(key(username, ipAddress));
        }
    }

    private static final class InMemoryLoginFailureStore implements AuthService.LoginFailureStore {
        private final Map<String, FailureWindow> attempts = new java.util.HashMap<>();

        /**
         * 在内存窗口中占用一次登录尝试
         */
        @Override
        public synchronized boolean reserveAttempt(String username, String ipAddress) {
            String key = key(username, ipAddress);
            long now = System.nanoTime();
            FailureWindow current = attempts.get(key);
            if (current == null || current.expiresAtNanos() <= now) {
                attempts.put(key, new FailureWindow(1, now + LOGIN_FAILURE_WINDOW.toNanos()));
                return true;
            }
            if (current.attempts() >= MAX_LOGIN_FAILURES) {
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
            attempts.remove(key(username, ipAddress));
        }

        private record FailureWindow(int attempts, long expiresAtNanos) {
        }
    }

    private static String key(String username, String ipAddress) {
        return "auth:login:failure:" + username + ':' + ipAddress;
    }

    private static final class RedisBackedSaTokenDao extends SaTokenDaoDefaultImpl {
        private final RedisTemplate<String, Object> redisTemplate;

        private RedisBackedSaTokenDao(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * 从 Redis 读取 Sa-Token 对象
         */
        @Override
        public Object getObject(String key) {
            return redisTemplate.opsForValue().get(key);
        }

        /**
         * 从 Redis 读取并转换 Sa-Token 对象类型
         */
        @Override
        public <T> T getObject(String key, Class<T> cs) {
            Object value = getObject(key);
            return value == null ? null : cs.cast(value);
        }

        /**
         * 将 Sa-Token 对象写入 Redis
         */
        @Override
        public void setObject(String key, Object value, long timeout) {
            if (timeout == SaTokenDao.NEVER_EXPIRE) {
                redisTemplate.opsForValue().set(key, value);
                return;
            }
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout));
        }

        /**
         * 在保留原过期时间的前提下更新 Sa-Token 对象
         */
        @Override
        public void updateObject(String key, Object value) {
            Long timeout = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (timeout == null || timeout == SaTokenDao.NOT_VALUE_EXPIRE) {
                return;
            }
            if (timeout == SaTokenDao.NEVER_EXPIRE) {
                redisTemplate.opsForValue().set(key, value);
                return;
            }
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout));
        }

        /**
         * 删除 Redis 中的 Sa-Token 对象
         */
        @Override
        public void deleteObject(String key) {
            redisTemplate.delete(key);
        }

        /**
         * 查询 Redis 中 Sa-Token 对象的剩余有效期
         */
        @Override
        public long getObjectTimeout(String key) {
            Long timeout = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return timeout == null ? SaTokenDao.NOT_VALUE_EXPIRE : timeout;
        }

        /**
         * 更新 Redis 中 Sa-Token 对象的剩余有效期
         */
        @Override
        public void updateObjectTimeout(String key, long timeout) {
            if (timeout == SaTokenDao.NEVER_EXPIRE) {
                redisTemplate.persist(key);
                return;
            }
            redisTemplate.expire(key, Duration.ofSeconds(timeout));
        }

        /**
         * 按 Sa-Token 前缀和关键字搜索 Redis 键
         */
        @Override
        public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
            if (size <= 0 || start >= MAX_TOKEN_SEARCH_KEYS) {
                return List.of();
            }
            int safeStart = Math.max(start, 0);
            int safeSize = Math.min(size, MAX_TOKEN_SEARCH_PAGE_SIZE);
            int scanLimit = Math.min(MAX_TOKEN_SEARCH_KEYS, safeStart + safeSize);
            List<String> values = new ArrayList<>(scanLimit);
            String pattern = prefix + "*" + (keyword == null ? "" : keyword) + "*";
            try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                    .match(pattern)
                    .count(Math.min(scanLimit, MAX_TOKEN_SEARCH_PAGE_SIZE))
                    .build())) {
                while (cursor.hasNext() && values.size() < scanLimit) {
                    values.add(cursor.next());
                }
            }
            if (values.isEmpty()) {
                return List.of();
            }
            values.sort(String::compareTo);
            int from = Math.min(safeStart, values.size());
            int to = Math.min(from + safeSize, values.size());
            return List.copyOf(values.subList(from, to));
        }
    }
}
