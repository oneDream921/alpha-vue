package io.github.onedream921.alphavue.modules.monitor.service;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.monitor.config.RedisManagementProperties;
import io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyMetadataVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyPageVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisOverviewVo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 运维台业务服务
 */
@Service
public class RedisManagementService {
    private final RedisKeyspace keyspace;
    private final RedisManagementProperties properties;

    public RedisManagementService(RedisKeyspace keyspace, RedisManagementProperties properties) {
        this.keyspace = keyspace;
        this.properties = properties;
    }

    /**
     * 查询受控 Redis 概览
     */
    public RedisOverviewVo overview() {
        RedisOverview overview = keyspace.overview();
        return new RedisOverviewVo(overview.redisVersion(), overview.uptimeSeconds(), overview.usedMemoryBytes(),
                overview.connectedClients(), overview.managedKeyCounts());
    }

    /**
     * 使用 Redis SCAN 游标查询受控前缀的键元数据
     */
    public RedisKeyPageVo page(RedisKeyQuery query) {
        requireAllowedPrefix(query.prefix());
        RedisScanResult result = keyspace.scan(query.prefix(), query.cursor(), query.count());
        List<RedisKeyMetadataVo> records = result.records().stream().map(this::toMetadata).toList();
        return new RedisKeyPageVo(records, result.nextCursor(), !"0".equals(result.nextCursor()));
    }

    /**
     * 查询受控 Redis 键的元数据
     */
    public RedisKeyMetadataVo metadata(String key) {
        requireAllowedKey(key);
        RedisKeyMetadata metadata = keyspace.metadata(key);
        if (metadata == null) {
            throw invalidRequest();
        }
        return toMetadata(metadata);
    }

    /**
     * 删除单个受控 Redis 键
     */
    public boolean delete(String key) {
        requireAllowedKey(key);
        return keyspace.delete(key);
    }

    private RedisKeyMetadataVo toMetadata(RedisKeyMetadata metadata) {
        requireAllowedKey(metadata.key());
        return new RedisKeyMetadataVo(metadata.key(), category(metadata.key()), metadata.type(), metadata.ttlSeconds(),
                metadata.sizeBytes(), true);
    }

    private void requireAllowedPrefix(String prefix) {
        if (!properties.getPrefixes().contains(prefix)) {
            throw invalidRequest();
        }
    }

    private void requireAllowedKey(String key) {
        if (key == null || properties.getPrefixes().stream().noneMatch(prefix -> key.startsWith(prefix)
                && key.length() > prefix.length())) {
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
        if (key.startsWith("satoken:")) {
            return "Sa-Token 会话";
        }
        return "认证数据";
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}

interface RedisKeyspace {
    RedisScanResult scan(String prefix, String cursor, int count);

    RedisKeyMetadata metadata(String key);

    boolean delete(String key);

    RedisOverview overview();
}

record RedisScanResult(List<RedisKeyMetadata> records, String nextCursor) {
    RedisScanResult {
        records = List.copyOf(records);
    }
}

record RedisKeyMetadata(String key, String type, Long ttlSeconds, Long sizeBytes) {
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
    public RedisScanResult scan(String prefix, String cursor, int count) {
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
