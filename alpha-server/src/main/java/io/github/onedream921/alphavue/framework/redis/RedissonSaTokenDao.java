package io.github.onedream921.alphavue.framework.redis;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.session.SaSession;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Redisson-backed Sa-Token DAO with opaque physical keys and a bounded index. */
public class RedissonSaTokenDao implements SaTokenDao {
    private static final int MAX_SEARCH_KEYS = 1_000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String PHYSICAL_KEY_VERSION = "sa-token-kryo5-v2\u0000";
    private static final String DATA_PREFIX = "alpha:sa-token:data:";
    private static final String INDEX_PREFIX = "alpha:sa-token:index:";

    private final RedissonClient client;
    private final Codec codec;

    public RedissonSaTokenDao(RedissonClient client, @Qualifier("redissonSaTokenCodec") Codec codec) {
        this.client = client;
        this.codec = codec;
    }

    @Override
    public String get(String key) {
        return client.<String>getBucket(dataKey(key)).get();
    }

    @Override
    public void set(String key, String value, long timeout) {
        if (timeout > 0) {
            client.<String>getBucket(dataKey(key)).set(value, timeout, TimeUnit.SECONDS);
        } else if (timeout == NEVER_EXPIRE) {
            client.<String>getBucket(dataKey(key)).set(value);
        }
        index(key, timeout);
    }

    @Override
    public void update(String key, String value) {
        long timeout = getTimeout(key);
        if (timeout == NEVER_EXPIRE) {
            set(key, value, NEVER_EXPIRE);
        } else if (timeout >= 0) {
            set(key, value, timeout);
        }
    }

    @Override
    public void delete(String key) {
        deletePhysical(key);
    }

    @Override
    public long getTimeout(String key) {
        return timeout(dataKey(key));
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        updatePhysicalTimeout(dataKey(key), timeout);
        updatePhysicalTimeout(indexKey(key), timeout);
    }

    @Override
    public Object getObject(String key) {
        return bucket(key).get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> classType) {
        Object value = getObject(key);
        return value == null ? null : (T) value;
    }

    @Override
    public void setObject(String key, Object value, long timeout) {
        setBucket(key, value, timeout);
        index(key, timeout);
    }

    @Override
    public void updateObject(String key, Object value) {
        long timeout = getObjectTimeout(key);
        if (timeout == NEVER_EXPIRE) {
            setObject(key, value, NEVER_EXPIRE);
        } else if (timeout >= 0) {
            setObject(key, value, timeout);
        }
    }

    @Override
    public void deleteObject(String key) {
        deletePhysical(key);
    }

    @Override
    public long getObjectTimeout(String key) {
        return timeout(dataKey(key));
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        updatePhysicalTimeout(dataKey(key), timeout);
        updatePhysicalTimeout(indexKey(key), timeout);
    }

    @Override
    public SaSession getSession(String sessionId) {
        return getObject(sessionId, SaSession.class);
    }

    @Override
    public void setSession(SaSession session, long timeout) {
        setObject(session.getId(), session, timeout);
    }

    @Override
    public void updateSession(SaSession session) {
        updateObject(session.getId(), session);
    }

    @Override
    public void deleteSession(String sessionId) {
        deleteObject(sessionId);
    }

    @Override
    public long getSessionTimeout(String sessionId) {
        return getObjectTimeout(sessionId);
    }

    @Override
    public void updateSessionTimeout(String sessionId, long timeout) {
        updateObjectTimeout(sessionId, timeout);
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        if (size == 0 || start < 0 || start >= MAX_SEARCH_KEYS) {
            return List.of();
        }
        int safeSize = size < 0 ? MAX_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int scanLimit = Math.min(MAX_SEARCH_KEYS, start + safeSize);
        List<String> matches = new ArrayList<>(scanLimit);
        RKeys keys = client.getKeys();
        KeysScanOptions options = KeysScanOptions.defaults()
                .pattern(INDEX_PREFIX + "*")
                .limit(MAX_SEARCH_KEYS)
                .chunkSize(Math.min(scanLimit, MAX_PAGE_SIZE));
        for (String indexKey : keys.getKeys(options)) {
            String logical = client.<String>getBucket(indexKey).get();
            if (logical != null && client.getBucket(dataKey(logical)).isExists()
                    && (prefix == null || logical.startsWith(prefix))
                    && (keyword == null || logical.contains(keyword))) {
                matches.add(logical);
            }
            if (matches.size() >= scanLimit) {
                break;
            }
        }
        matches.sort(sortType ? Comparator.naturalOrder() : Comparator.reverseOrder());
        int from = Math.min(start, matches.size());
        int to = Math.min(from + safeSize, matches.size());
        return List.copyOf(matches.subList(from, to));
    }

    private RBucket<Object> bucket(String key) {
        return client.getBucket(dataKey(key), codec);
    }

    private void setBucket(String key, Object value, long timeout) {
        RBucket<Object> bucket = bucket(key);
        if (timeout > 0) {
            bucket.set(value, timeout, TimeUnit.SECONDS);
        } else if (timeout == NEVER_EXPIRE) {
            bucket.set(value);
        }
    }

    private long timeout(String physicalKey) {
        long ttl = client.getBucket(physicalKey).remainTimeToLive();
        if (!client.getBucket(physicalKey).isExists()) {
            return NOT_VALUE_EXPIRE;
        }
        return ttl < 0 ? NEVER_EXPIRE : TimeUnit.MILLISECONDS.toSeconds(ttl);
    }

    private void updatePhysicalTimeout(String physicalKey, long timeout) {
        RBucket<Object> bucket = client.getBucket(physicalKey, codec);
        if (timeout == NEVER_EXPIRE) {
            bucket.clearExpire();
        } else if (timeout > 0) {
            bucket.expire(Duration.ofSeconds(timeout));
        }
    }

    private void index(String logicalKey, long timeout) {
        RBucket<String> index = client.getBucket(indexKey(logicalKey));
        if (timeout > 0) {
            index.set(logicalKey, timeout, TimeUnit.SECONDS);
        } else if (timeout == NEVER_EXPIRE) {
            index.set(logicalKey);
        }
    }

    private void deletePhysical(String logicalKey) {
        client.getBucket(dataKey(logicalKey), codec).delete();
        client.getBucket(indexKey(logicalKey)).delete();
    }

    private static String dataKey(String logicalKey) {
        return DATA_PREFIX + RedisPhysicalKey.sha256(PHYSICAL_KEY_VERSION + logicalKey);
    }

    private static String indexKey(String logicalKey) {
        return INDEX_PREFIX + RedisPhysicalKey.sha256(PHYSICAL_KEY_VERSION + logicalKey);
    }

}
