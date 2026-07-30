package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.ConfigRequests;
import io.github.onedream921.alphavue.modules.system.config.RuntimeConfigBinding;
import io.github.onedream921.alphavue.modules.system.entity.SysConfigDefinition;
import io.github.onedream921.alphavue.modules.system.mapper.SysConfigDefinitionMapper;
import io.github.onedream921.alphavue.modules.system.entity.SysConfig;
import io.github.onedream921.alphavue.modules.system.mapper.SysConfigMapper;
import io.github.onedream921.alphavue.modules.system.vo.ConfigVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 参数配置业务服务
 */
@Service
public class ConfigService {
    private final SysConfigMapper configMapper;
    private final ConfigCacheStore configCacheStore;
    private final SysConfigDefinitionMapper definitionMapper;

    public ConfigService(SysConfigMapper configMapper, ConfigCacheStore configCacheStore, SysConfigDefinitionMapper definitionMapper) {
        this.configMapper = configMapper;
        this.configCacheStore = configCacheStore;
        this.definitionMapper = definitionMapper;
    }

    /**
     * 分页查询参数配置，不触发任何运行时配置刷新
     */
    public PageResponse<ConfigVo> page(int pageNumber, int pageSize) {
        Page<SysConfig> page = configMapper.selectPagePublished(new Page<>(pageNumber, pageSize));
        if (page.getRecords().isEmpty()) {
            return new PageResponse<>(java.util.List.of(), page.getTotal(), pageNumber, pageSize);
        }
        Map<String, SysConfigDefinition> definitions = definitionMapper.selectPublishedByKeys(
                        page.getRecords().stream().map(SysConfig::getConfigKey).toList()).stream()
                .collect(Collectors.toMap(SysConfigDefinition::getConfigKey, Function.identity()));
        return new PageResponse<>(page.getRecords().stream()
                        .map(config -> ConfigVo.from(config, definitions.get(config.getConfigKey()))).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询单个参数配置详情
     */
    public ConfigVo get(long id) {
        SysConfig config = requireConfig(id);
        return ConfigVo.from(config, requirePublished(config.getConfigKey()));
    }

    /**
     * 创建参数配置并在提交后发布运行时缓存
     */
    @Transactional
    public ConfigVo create(ConfigRequests.Save request) {
        SysConfigDefinition definition = requirePublished(request.configKey());
        String configKey = definition.getConfigKey();
        validate(definition, request.configValue());
        if (configMapper.selectActiveByConfigKey(configKey) != null) {
            throw invalidRequest();
        }
        SysConfig config = new SysConfig();
        copy(request, config, definition);
        configMapper.insertConfig(config);
        SysConfig saved = requireConfig(config.getId());
        publishCache(saved);
        return ConfigVo.from(saved, definition);
    }

    /**
     * 更新参数配置并在提交后发布运行时缓存
     */
    @Transactional
    public ConfigVo update(long id, ConfigRequests.Save request) {
        SysConfig config = requireConfig(id);
        String previousKey = config.getConfigKey();
        SysConfigDefinition definition = requirePublished(request.configKey());
        if (!previousKey.equals(definition.getConfigKey())) {
            throw invalidRequest();
        }
        validate(definition, request.configValue());
        copy(request, config, definition);
        if (configMapper.updateConfig(config) != 1) {
            throw invalidRequest();
        }
        evictCache(previousKey);
        SysConfig saved = requireConfig(id);
        publishCache(saved);
        return ConfigVo.from(saved, definition);
    }

    /**
     * 软删除指定参数配置
     */
    @Transactional
    public void delete(long id) {
        SysConfig config = requireConfig(id);
        requirePublished(config.getConfigKey());
        if (configMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
        evictCache(config.getConfigKey());
    }

    /**
     * 按键读取业务参数，并优先使用缓存。
     */
    public String value(RuntimeConfigBinding binding) {
        SysConfigDefinition definition = definitionMapper.selectPublishedByBinding(binding.name());
        if (definition == null) throw invalidRequest();
        String cached = configCacheStore.get(definition.getConfigKey());
        if (cached != null) { validate(definition, cached); return cached; }
        SysConfig config = configMapper.selectActiveByConfigKey(definition.getConfigKey());
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) return definition.getDefaultValue();
        validate(definition, config.getConfigValue());
        configCacheStore.put(definition.getConfigKey(), config.getConfigValue());
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
        if (config == null || definitionMapper.selectPublishedByKey(config.getConfigKey()) == null) {
            throw invalidRequest();
        }
        return config;
    }

    private static void copy(ConfigRequests.Save request, SysConfig config, SysConfigDefinition definition) {
        config.setConfigName(definition.getConfigName());
        config.setConfigKey(definition.getConfigKey());
        config.setConfigValue(request.configValue());
        config.setConfigGroup(definition.getConfigGroup());
        config.setDataType(definition.getValueType());
        config.setEnabled(request.enabled());
        config.setDescription(null);
    }

    private SysConfigDefinition requirePublished(String configKey) {
        SysConfigDefinition definition = definitionMapper.selectPublishedByKey(configKey == null ? "" : configKey.trim());
        if (definition == null) throw invalidRequest();
        return definition;
    }

    private static void validate(SysConfigDefinition definition, String raw) {
        try {
            if (raw == null) throw invalidRequest();
            switch (definition.getValueType()) {
                case "BOOLEAN" -> { if (!"true".equals(raw) && !"false".equals(raw)) throw invalidRequest(); }
                case "INTEGER" -> { int value = Integer.parseInt(raw); if (definition.getIntegerMin() != null && value < definition.getIntegerMin() || definition.getIntegerMax() != null && value > definition.getIntegerMax()) throw invalidRequest(); }
                case "ENUM" -> { if (definition.getEnumValues() == null || !java.util.Arrays.asList(definition.getEnumValues().split(",")).contains(raw)) throw invalidRequest(); }
                case "STRING" -> { if (definition.getStringMaxLength() != null && raw.length() > definition.getStringMaxLength() || definition.getStringPattern() != null && !raw.matches(definition.getStringPattern())) throw invalidRequest(); }
                default -> throw invalidRequest();
            }
        } catch (NumberFormatException exception) { throw invalidRequest(); }
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}
