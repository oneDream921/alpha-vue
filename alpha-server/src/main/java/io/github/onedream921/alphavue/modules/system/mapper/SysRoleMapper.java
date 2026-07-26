package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色基础数据访问 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    int softDeleteById(@Param("id") long id);
}
