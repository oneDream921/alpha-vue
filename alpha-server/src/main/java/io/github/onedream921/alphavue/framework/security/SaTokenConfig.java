package io.github.onedream921.alphavue.framework.security;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import io.github.onedream921.alphavue.modules.auth.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Sa-Token HTTP protection, Redis session storage, and test-safe login limits. */
@Configuration
@EnableAsync
public class SaTokenConfig implements WebMvcConfigurer {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/actuator/health",
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.ico",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/**/*.svg",
                        "/webjars/**");
    }

    /**
     * Production sessions are shared through Redis. The test profile deliberately
     * leaves Sa-Token on its built-in in-memory DAO so tests run without Docker.
     */
    @Bean
    @Profile("!test")
    SaTokenDao redisSaTokenDao(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.afterPropertiesSet();
        return new RedisBackedSaTokenDao(template);
    }

    @Bean
    @Profile("!test")
    AuthService.LoginFailureStore redisLoginFailureStore(StringRedisTemplate redisTemplate) {
        return new RedisLoginFailureStore(redisTemplate);
    }

    @Bean
    @Profile("test")
    AuthService.LoginFailureStore inMemoryLoginFailureStore() {
        return new InMemoryLoginFailureStore();
    }

    private static final class RedisLoginFailureStore implements AuthService.LoginFailureStore {
        private final StringRedisTemplate redisTemplate;

        private RedisLoginFailureStore(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @Override
        public boolean isLocked(String username, String ipAddress) {
            String attempts = redisTemplate.opsForValue().get(key(username, ipAddress));
            return attempts != null && Integer.parseInt(attempts) >= MAX_LOGIN_FAILURES;
        }

        @Override
        public void recordFailure(String username, String ipAddress) {
            String key = key(username, ipAddress);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, LOGIN_FAILURE_WINDOW);
            }
        }

        @Override
        public void clear(String username, String ipAddress) {
            redisTemplate.delete(key(username, ipAddress));
        }
    }

    private static final class InMemoryLoginFailureStore implements AuthService.LoginFailureStore {
        private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

        @Override
        public boolean isLocked(String username, String ipAddress) {
            return attempts.getOrDefault(key(username, ipAddress), 0) >= MAX_LOGIN_FAILURES;
        }

        @Override
        public void recordFailure(String username, String ipAddress) {
            attempts.merge(key(username, ipAddress), 1, Integer::sum);
        }

        @Override
        public void clear(String username, String ipAddress) {
            attempts.remove(key(username, ipAddress));
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

        @Override
        public Object getObject(String key) {
            return redisTemplate.opsForValue().get(key);
        }

        @Override
        public <T> T getObject(String key, Class<T> cs) {
            Object value = getObject(key);
            return value == null ? null : cs.cast(value);
        }

        @Override
        public void setObject(String key, Object value, long timeout) {
            if (timeout == SaTokenDao.NEVER_EXPIRE) {
                redisTemplate.opsForValue().set(key, value);
                return;
            }
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout));
        }

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

        @Override
        public void deleteObject(String key) {
            redisTemplate.delete(key);
        }

        @Override
        public long getObjectTimeout(String key) {
            Long timeout = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return timeout == null ? SaTokenDao.NOT_VALUE_EXPIRE : timeout;
        }

        @Override
        public void updateObjectTimeout(String key, long timeout) {
            if (timeout == SaTokenDao.NEVER_EXPIRE) {
                redisTemplate.persist(key);
                return;
            }
            redisTemplate.expire(key, Duration.ofSeconds(timeout));
        }

        @Override
        public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
            Set<String> keys = redisTemplate.keys(prefix + "*" + keyword + "*");
            if (keys == null || keys.isEmpty() || size <= 0) {
                return List.of();
            }
            List<String> values = new ArrayList<>(keys);
            values.sort(String::compareTo);
            int from = Math.min(Math.max(start, 0), values.size());
            int to = Math.min(from + size, values.size());
            return List.copyOf(values.subList(from, to));
        }
    }
}
