package io.github.onedream921.alphavue.framework.redis;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.SaTokenInfo;
import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Vector;

/**
 * Redisson 编解码边界。
 *
 * <p>默认只处理字符串；需要对象时必须显式选择对应白名单 Codec。</p>
 */
@Configuration
public class RedissonCodecRegistry {

    private static final Set<String> CACHE_TYPES = Set.of(
            String.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Boolean.class.getName(),
            ArrayList.class.getName(),
            List.class.getName(),
            EnabledDictItemVo.class.getName());

    private static final Set<String> SA_TOKEN_TYPES = Set.of(
            String.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Boolean.class.getName(),
            ArrayList.class.getName(),
            HashMap.class.getName(),
            HashSet.class.getName(),
            LinkedHashMap.class.getName(),
            ConcurrentHashMap.class.getName(),
            Vector.class.getName(),
            SaSession.class.getName(),
            SaTerminalInfo.class.getName(),
            SaTokenInfo.class.getName());

    @Bean
    StringCodec redissonStringCodec() {
        return StringCodec.INSTANCE;
    }

    @Bean("redissonCacheCodec")
    Codec redissonCacheCodec() {
        return new Kryo5Codec(CACHE_TYPES, false);
    }

    @Bean("redissonSaTokenCodec")
    Codec redissonSaTokenCodec() {
        return new Kryo5Codec(SA_TOKEN_TYPES, false);
    }
}
