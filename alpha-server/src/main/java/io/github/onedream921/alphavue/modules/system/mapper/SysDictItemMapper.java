package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.system.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典项数据访问 Mapper
 */
@Mapper
public interface SysDictItemMapper {
    Page<SysDictItem> selectPageActiveByTypeId(Page<SysDictItem> page, @Param("typeId") long typeId);

    SysDictItem selectActiveById(@Param("id") long id);

    SysDictItem selectActiveByTypeIdAndValue(@Param("typeId") long typeId, @Param("value") String value);

    List<SysDictItem> selectEnabledByTypeCode(@Param("typeCode") String typeCode);

    int countActiveByTypeId(@Param("typeId") long typeId);

    int insertItem(@Param("item") SysDictItem item);

    int updateItem(@Param("item") SysDictItem item);

    int softDeleteById(@Param("id") long id);
}
