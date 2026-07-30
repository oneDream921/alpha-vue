package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.system.entity.SysConfigDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SysConfigDefinitionMapper {
    Page<SysConfigDefinition> selectPageActive(Page<SysConfigDefinition> page);
    SysConfigDefinition selectActiveById(@Param("id") long id);
    SysConfigDefinition selectPublishedByKey(@Param("configKey") String configKey);
    SysConfigDefinition selectPublishedByBinding(@Param("runtimeBinding") String runtimeBinding);
    SysConfigDefinition selectActiveByKey(@Param("configKey") String configKey);
    SysConfigDefinition selectActiveByBinding(@Param("runtimeBinding") String runtimeBinding);
    List<SysConfigDefinition> selectPublishedByKeys(@Param("configKeys") Collection<String> configKeys);
    int insertDefinition(@Param("definition") SysConfigDefinition definition);
    int updateDefinition(@Param("definition") SysConfigDefinition definition);
}
