package io.github.onedream921.alphavue.modules.auth.config;

import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
public class AjCaptchaConfig {
    private static final Logger log = LoggerFactory.getLogger(AjCaptchaConfig.class);
    private static final String CACHE_TYPE = "alphaRedis";

    @Bean
    CaptchaService ajCaptchaService(ObjectProvider<RedissonClient> clients) {
        RedissonClient redissonClient = clients.getIfAvailable();
        String cacheType = redissonClient == null ? "local" : CACHE_TYPE;
        if (redissonClient != null) CaptchaServiceFactory.cacheService.put(CACHE_TYPE, new RedisCaptchaCache(redissonClient));
        Properties properties = new Properties();
        properties.setProperty("captcha.type", "blockPuzzle");
        properties.setProperty("captcha.cacheType", cacheType);
        properties.setProperty("captcha.init.original", "false");
        properties.setProperty("captcha.water.mark", "Alpha Vue");
        // Allow small frontend scaling and pointer rounding differences while keeping strict puzzle validation.
        properties.setProperty("captcha.slip.offset", "20");
        properties.setProperty("captcha.aes.status", "true");
        // Do not render decoy puzzle outlines: users must have one unambiguous target gap.
        properties.setProperty("captcha.interference.options", "2");
        log.info("AJ-Captcha blockPuzzle initialized: cacheType={}, slipOffset={}, interferenceOptions={}, aesEnabled={}",
                cacheType, properties.getProperty("captcha.slip.offset"),
                properties.getProperty("captcha.interference.options"), properties.getProperty("captcha.aes.status"));
        return CaptchaServiceFactory.getInstance(properties);
    }

    private static final class RedisCaptchaCache implements CaptchaCacheService {
        private static final String PREFIX = "alpha:captcha:slider:";
        private final RedissonClient client;

        private RedisCaptchaCache(RedissonClient client) { this.client = client; }
        private RBucket<String> bucket(String key) { return client.getBucket(PREFIX + key, StringCodec.INSTANCE); }
        @Override public void set(String key, String value, long seconds) { bucket(key).set(value, seconds, TimeUnit.SECONDS); }
        @Override public boolean exists(String key) { return bucket(key).isExists(); }
        @Override public void delete(String key) { bucket(key).delete(); }
        @Override public String get(String key) { return bucket(key).get(); }
        @Override public String type() { return CACHE_TYPE; }
        @Override public Long increment(String key, long value) {
            RMap<String, String> map = client.getMap(PREFIX + "counter", StringCodec.INSTANCE);
            return Long.parseLong(map.compute(key, (ignored, current) -> String.valueOf((current == null ? 0 : Long.parseLong(current)) + value)));
        }
        @Override public void setExpire(String key, long seconds) { bucket(key).expire(seconds, TimeUnit.SECONDS); }
    }
}
