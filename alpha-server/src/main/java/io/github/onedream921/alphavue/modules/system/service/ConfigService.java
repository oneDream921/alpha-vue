package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.ConfigRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysConfig;
import io.github.onedream921.alphavue.modules.system.mapper.SysConfigMapper;
import io.github.onedream921.alphavue.modules.system.vo.ConfigVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.Set;

/**
 * 参数配置业务服务
 */
@Service
public class ConfigService {
    private static final Set<String> FORBIDDEN_PREFIXES = Set.of(
            "spring.", "server.", "datasource.", "redis.", "minio.", "sa-token.");
    private static final String SENSITIVE_SEGMENT_PATTERN =
            "(^|[._-])(password|passwd|secret|token|credential|key|private[-_]?key|api[-_]?key|access[-_]?key)([._-]|$)";

    private final SysConfigMapper configMapper;
    private final ConfigCacheStore configCacheStore;

    public ConfigService(SysConfigMapper configMapper, ConfigCacheStore configCacheStore) {
        this.configMapper = configMapper;
        this.configCacheStore = configCacheStore;
    }

    /**
     * 分页查询参数配置，不触发任何运行时配置刷新
     */
    public PageResponse<ConfigVo> page(int pageNumber, int pageSize) {
        Page<SysConfig> page = configMapper.selectPageActive(new Page<>(pageNumber, pageSize));
        return new PageResponse<>(page.getRecords().stream().map(ConfigVo::from).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询单个参数配置详情
     */
    public ConfigVo get(long id) {
        return ConfigVo.from(requireConfig(id));
    }

    /**
     * 创建参数配置并在提交后发布运行时缓存
     */
    @Transactional
    public ConfigVo create(ConfigRequests.Save request) {
        String configKey = validateConfigKey(request.configKey());
        if (configMapper.selectActiveByConfigKey(configKey) != null) {
            throw invalidRequest();
        }
        SysConfig config = new SysConfig();
        copy(request, config, configKey);
        configMapper.insertConfig(config);
        SysConfig saved = requireConfig(config.getId());
        publishCache(saved);
        return ConfigVo.from(saved);
    }

    /**
     * 更新参数配置并在提交后发布运行时缓存
     */
    @Transactional
    public ConfigVo update(long id, ConfigRequests.Save request) {
        SysConfig config = requireConfig(id);
        String previousKey = config.getConfigKey();
        String configKey = validateConfigKey(request.configKey());
        SysConfig existing = configMapper.selectActiveByConfigKey(configKey);
        if (existing != null && !existing.getId().equals(id)) {
            throw invalidRequest();
        }
        copy(request, config, configKey);
        if (configMapper.updateConfig(config) != 1) {
            throw invalidRequest();
        }
        evictCache(previousKey);
        if (!previousKey.equals(configKey)) evictCache(configKey);
        SysConfig saved = requireConfig(id);
        publishCache(saved);
        return ConfigVo.from(saved);
    }

    /**
     * 软删除指定参数配置
     */
    @Transactional
    public void delete(long id) {
        SysConfig config = requireConfig(id);
        if (configMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
        evictCache(config.getConfigKey());
    }

    /**
     * 按键读取业务参数，并优先使用缓存。
     */
    public String value(String configKey) {
        String normalizedKey = configKey.trim();
        String cached = configCacheStore.get(normalizedKey);
        if (cached != null) return cached;
        SysConfig config = configMapper.selectActiveByConfigKey(normalizedKey);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) return null;
        configCacheStore.put(normalizedKey, config.getConfigValue());
        return config.getConfigValue();
    }

    private void evictCache(String configKey) {
        afterCommit(() -> configCacheStore.evict(configKey));
    }

    private void publishCache(SysConfig config) {
        afterCommit(() -> {
            if (Boolean.TRUE.equals(config.getEnabled())) {
                configCacheStore.put(config.getConfigKey(), config.getConfigValue());
            } else {
                configCacheStore.evict(config.getConfigKey());
            }
        });
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else action.run();
    }

    private SysConfig requireConfig(long id) {
        SysConfig config = configMapper.selectActiveById(id);
        if (config == null) {
            throw invalidRequest();
        }
        return config;
    }

    private static String validateConfigKey(String value) {
        String configKey = value.trim();
        String normalized = configKey.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_PREFIXES.stream().anyMatch(normalized::startsWith)
                || normalized.matches(".*" + SENSITIVE_SEGMENT_PATTERN + ".*")) {
            throw invalidRequest();
        }
        return configKey;
    }

    private static void copy(ConfigRequests.Save request, SysConfig config, String configKey) {
        config.setConfigName(request.configName().trim());
        config.setConfigKey(configKey);
        config.setConfigValue(request.configValue());
        config.setConfigGroup(request.configGroup().trim());
        config.setDataType(request.dataType());
        config.setEnabled(request.enabled());
        config.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}
