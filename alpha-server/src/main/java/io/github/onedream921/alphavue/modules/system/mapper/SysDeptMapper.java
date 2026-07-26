package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 部门基础数据访问 Mapper
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
    int countEnabledById(@Param("id") long id);
}
