package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.system.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型数据访问 Mapper
 */
@Mapper
public interface SysDictTypeMapper {
    /**
     * 分页查询未删除的字典类型
     */
    Page<SysDictType> selectPageActive(Page<SysDictType> page);

    /**
     * 按主键查询未删除的字典类型
     */
    SysDictType selectActiveById(@Param("id") long id);

    /**
     * 按类型编码查询未删除的字典类型
     */
    SysDictType selectActiveByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 新增字典类型
     */
    int insertType(@Param("type") SysDictType type);

    /**
     * 更新字典类型
     */
    int updateType(@Param("type") SysDictType type);

    /**
     * 逻辑删除字典类型
     */
    int softDeleteById(@Param("id") long id);
}
