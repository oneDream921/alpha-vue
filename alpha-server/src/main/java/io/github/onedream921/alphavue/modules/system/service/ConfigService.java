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

    public ConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
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
     * 创建参数配置，仅保存数据且不会自动生效
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
        return ConfigVo.from(config);
    }

    /**
     * 更新参数配置，仅保存数据且不会自动生效
     */
    @Transactional
    public ConfigVo update(long id, ConfigRequests.Save request) {
        SysConfig config = requireConfig(id);
        String configKey = validateConfigKey(request.configKey());
        SysConfig existing = configMapper.selectActiveByConfigKey(configKey);
        if (existing != null && !existing.getId().equals(id)) {
            throw invalidRequest();
        }
        copy(request, config, configKey);
        if (configMapper.updateConfig(config) != 1) {
            throw invalidRequest();
        }
        return ConfigVo.from(config);
    }

    /**
     * 软删除指定参数配置
     */
    @Transactional
    public void delete(long id) {
        requireConfig(id);
        if (configMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
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
        config.setConfigKey(configKey);
        config.setConfigValue(request.configValue());
        config.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}
