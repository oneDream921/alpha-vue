package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery;
import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyMetadataVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyPageVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisOverviewVo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Redis 运维台业务服务
 */
@Service
public class RedisManagementService {
    private static final int MAX_SCAN_ROUNDS_PER_PAGE = 1_000;

    private final RedisKeyspace keyspace;
    private final RedisManagementProperties properties;

    public RedisManagementService(RedisKeyspace keyspace, RedisManagementProperties properties) {
        this.keyspace = keyspace;
        this.properties = properties;
    }

    /**
     * 查询 Redis 概览
     */
    public RedisOverviewVo overview() {
        RedisOverview overview = keyspace.overview();
        return new RedisOverviewVo(overview.redisVersion(), overview.uptimeSeconds(), overview.usedMemoryBytes(),
                overview.connectedClients(), overview.managedKeyCounts());
    }

    /**
     * 使用 Redis SCAN 游标查询 Redis 键元数据
     */
    public RedisKeyPageVo page(RedisKeyQuery query) {
        String prefix = normalizedPrefix(query.prefix());
        String keyword = normalizedKeyword(query.keyword());
        String cursor = query.cursor();
        List<RedisKeyMetadataVo> records = new ArrayList<>();
        int scanRounds = 0;

        do {
            RedisScanResult result = keyspace.scan(prefix, keyword, cursor, query.count());
            cursor = result.nextCursor();
            result.records().forEach(metadata -> records.add(toMetadata(metadata)));
            scanRounds++;
        } while (records.size() < query.count() && !"0".equals(cursor) && scanRounds < MAX_SCAN_ROUNDS_PER_PAGE);

        return new RedisKeyPageVo(records, cursor, !"0".equals(cursor));
    }

    /**
     * 查询 Redis 键的元数据
     */
    public RedisKeyMetadataVo metadata(String key) {
        requireReadableKey(key);
        RedisKeyMetadata metadata = keyspace.metadata(key);
        if (metadata == null) {
            throw invalidRequest();
        }
        return toMetadata(metadata);
    }

    /**
     * 删除单个 Redis 键
     */
    public boolean delete(String key) {
        requireReadableKey(key);
        return keyspace.delete(key);
    }

    private RedisKeyMetadataVo toMetadata(RedisKeyMetadata metadata) {
        requireReadableKey(metadata.key());
        boolean masked = properties.isMaskValues() || isSensitiveKey(metadata.key());
        return new RedisKeyMetadataVo(metadata.key(), category(metadata.key()), metadata.type(), metadata.ttlSeconds(),
                metadata.sizeBytes(), masked ? "[masked]" : metadata.value(), masked || metadata.valueTruncated());
    }

    private void requireReadableKey(String key) {
        if (key == null || key.isBlank()) {
            throw invalidRequest();
        }
    }

    private static String category(String key) {
        if (key.startsWith("auth:captcha:")) {
            return "验证码";
        }
        if (key.startsWith("auth:login:failure:")) {
            return "登录失败窗口";
        }
        if (key.startsWith("system:dict:")) {
            return "数据字典缓存";
        }
        if (key.startsWith("satoken:")
                || key.contains(":login:session:")
                || key.contains(":login:token:")
                || key.contains(":login:last-active:")) {
            return "Sa-Token 会话";
        }
        if (key.startsWith("spring:")) {
            return "Spring 数据";
        }
        return "业务/缓存数据";
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.startsWith("auth:captcha:")
                || normalized.contains("login:session:")
                || normalized.contains("login:token:")
                || normalized.startsWith("satoken:")
                || normalized.matches(".*(?:password|passwd|secret|token|credential|private[-_]?key|api[-_]?key).*" );
    }

    private static String normalizedPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.trim();
    }

    private static String normalizedKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}

interface RedisKeyspace {
    RedisScanResult scan(String prefix, String keyword, String cursor, int count);

    RedisKeyMetadata metadata(String key);

    boolean delete(String key);

    RedisOverview overview();
}

record RedisScanResult(List<RedisKeyMetadata> records, String nextCursor) {
    RedisScanResult {
        records = List.copyOf(records);
    }
}

record RedisKeyMetadata(String key, String type, Long ttlSeconds, Long sizeBytes, String value, boolean valueTruncated) {
}

record RedisOverview(String redisVersion, Long uptimeSeconds, Long usedMemoryBytes, Long connectedClients,
                     java.util.Map<String, Long> managedKeyCounts) {
    RedisOverview {
        managedKeyCounts = java.util.Map.copyOf(managedKeyCounts);
    }
}

/**
 * 测试环境 Redis 键空间占位实现，避免测试依赖外部 Redis
 */
@Service
@Profile("test")
class TestRedisKeyspace implements RedisKeyspace {
    @Override
    public RedisScanResult scan(String prefix, String keyword, String cursor, int count) {
        return new RedisScanResult(List.of(), "0");
    }

    @Override
    public RedisKeyMetadata metadata(String key) {
        return null;
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public RedisOverview overview() {
        return new RedisOverview(null, null, null, null, java.util.Map.of());
    }
}
