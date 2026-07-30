package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 参数配置数据访问 Mapper
 */
@Mapper
public interface SysConfigMapper {
    /**
     * 分页查询未删除的参数配置
     */
    Page<SysConfig> selectPageActive(Page<SysConfig> page);

    /**
     * 分页查询存在已发布定义的参数配置。
     */
    Page<SysConfig> selectPagePublished(Page<SysConfig> page);

    /**
     * 按主键查询未删除的参数配置
     */
    SysConfig selectActiveById(@Param("id") long id);

    /**
     * 按配置键查询未删除的参数配置
     */
    SysConfig selectActiveByConfigKey(@Param("configKey") String configKey);

    /**
     * 新增参数配置
     */
    int insertConfig(@Param("config") SysConfig config);

    /**
     * 更新参数配置
     */
    int updateConfig(@Param("config") SysConfig config);

    /**
     * 软删除参数配置
     */
    int softDeleteById(@Param("id") long id);
}
