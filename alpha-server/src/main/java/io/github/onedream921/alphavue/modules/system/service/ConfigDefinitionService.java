package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.config.RuntimeConfigBinding;
import io.github.onedream921.alphavue.modules.system.dto.ConfigRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysConfig;
import io.github.onedream921.alphavue.modules.system.entity.SysConfigDefinition;
import io.github.onedream921.alphavue.modules.system.mapper.SysConfigMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysConfigDefinitionMapper;
import io.github.onedream921.alphavue.modules.system.vo.ConfigDefinitionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 受控业务配置定义目录。 */
@Service
public class ConfigDefinitionService {
    private static final java.util.regex.Pattern SENSITIVE_KEY_SEGMENT = java.util.regex.Pattern.compile(
            "(^|[._-])(password|passwd|secret|token|credential|key|private[-_]?key|api[-_]?key|access[-_]?key)([._-]|$)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private final SysConfigDefinitionMapper mapper;
    private final SysConfigMapper configMapper;

    public ConfigDefinitionService(SysConfigDefinitionMapper mapper, SysConfigMapper configMapper) {
        this.mapper = mapper;
        this.configMapper = configMapper;
    }
    public PageResponse<ConfigDefinitionVo> page(int page, int size) {
        Page<SysConfigDefinition> result = mapper.selectPageActive(new Page<>(page, size));
        return new PageResponse<>(result.getRecords().stream().map(ConfigDefinitionVo::from).toList(), result.getTotal(), page, size);
    }
    @Transactional
    public ConfigDefinitionVo create(ConfigRequests.DefinitionSave request) {
        if (mapper.selectActiveByKey(request.configKey()) != null) throw invalid();
        SysConfigDefinition definition = copy(request, new SysConfigDefinition());
        requireAvailableBinding(definition, null);
        if (mapper.insertDefinition(definition) != 1) throw invalid();
        return ConfigDefinitionVo.from(definition);
    }
    @Transactional
    public ConfigDefinitionVo update(long id, ConfigRequests.DefinitionSave request) {
        SysConfigDefinition current = mapper.selectActiveById(id);
        if (current == null || !current.getConfigKey().equals(request.configKey()) || !current.getValueType().equals(request.valueType())
                || !current.getSensitive().equals(request.sensitive()) || !current.getDynamic().equals(request.dynamic())
                || !same(current.getRuntimeBinding(), request.runtimeBinding())) throw invalid();
        copy(request, current);
        requireAvailableBinding(current, current.getId());
        validateExistingValue(current);
        if (mapper.updateDefinition(current) != 1) throw invalid();
        return ConfigDefinitionVo.from(current);
    }
    private static SysConfigDefinition copy(ConfigRequests.DefinitionSave request, SysConfigDefinition target) {
        if (!"file".equals(request.configKey().split("\\.")[0]) || SENSITIVE_KEY_SEGMENT.matcher(request.configKey()).find()) throw invalid();
        if (Boolean.TRUE.equals(request.sensitive()) && Boolean.TRUE.equals(request.dynamic())) throw invalid();
        if (Boolean.TRUE.equals(request.dynamic())) {
            RuntimeConfigBinding binding = binding(request.runtimeBinding());
            if (!binding.configKey().equals(request.configKey()) || !binding.valueType().name().equals(request.valueType())
                    || !"PUBLISHED".equals(request.status())) throw invalid();
        } else if (request.runtimeBinding() != null && !request.runtimeBinding().isBlank()) throw invalid();
        String defaultValue = request.defaultValue();
        if ((defaultValue == null || defaultValue.isBlank()) && target.getId() != null && Boolean.TRUE.equals(target.getSensitive())) {
            defaultValue = target.getDefaultValue();
        }
        if (defaultValue == null || defaultValue.isBlank()) throw invalid();
        target.setConfigKey(request.configKey()); target.setConfigName(request.configName().trim()); target.setConfigGroup("file");
        target.setValueType(request.valueType()); target.setDefaultValue(defaultValue); target.setIntegerMin(request.integerMin()); target.setIntegerMax(request.integerMax());
        target.setStringMaxLength(request.stringMaxLength()); target.setStringPattern(request.stringPattern()); target.setEnumValues(request.enumValues());
        target.setSensitive(request.sensitive()); target.setDynamic(request.dynamic()); target.setRuntimeBinding(request.runtimeBinding()); target.setStatus(request.status());
        validate(target, defaultValue); return target;
    }
    private void requireAvailableBinding(SysConfigDefinition definition, Long excludedId) {
        if (!Boolean.TRUE.equals(definition.getDynamic())) return;
        SysConfigDefinition assigned = mapper.selectActiveByBinding(definition.getRuntimeBinding());
        if (assigned != null && !Objects.equals(assigned.getId(), excludedId)) throw invalid();
    }

    private void validateExistingValue(SysConfigDefinition definition) {
        SysConfig config = configMapper.selectActiveByConfigKey(definition.getConfigKey());
        if (config != null) validate(definition, config.getConfigValue());
    }

    private static RuntimeConfigBinding binding(String value) {
        try {
            return RuntimeConfigBinding.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalid();
        }
    }

    private static void validate(SysConfigDefinition d, String raw) {
        try { switch (d.getValueType()) {
            case "BOOLEAN" -> { if (!"true".equals(raw) && !"false".equals(raw)) throw invalid(); }
            case "INTEGER" -> { int v=Integer.parseInt(raw); if (d.getIntegerMin()!=null && v<d.getIntegerMin() || d.getIntegerMax()!=null && v>d.getIntegerMax()) throw invalid(); }
            case "ENUM" -> { if (d.getEnumValues()==null || !java.util.Arrays.asList(d.getEnumValues().split(",")).contains(raw)) throw invalid(); }
            case "STRING" -> {
                if (d.getStringMaxLength()!=null && raw.length()>d.getStringMaxLength()) throw invalid();
                if (d.getStringPattern()!=null && !d.getStringPattern().isBlank() && !Pattern.compile(d.getStringPattern()).matcher(raw).matches()) throw invalid();
            }
            default -> throw invalid();
        }} catch (NumberFormatException | PatternSyntaxException e) { throw invalid(); }
    }
    private static boolean same(String a,String b){return java.util.Objects.equals(a,b);}
    private static BusinessException invalid(){return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);}
}
