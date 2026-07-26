package io.github.onedream921.alphavue.modules.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 用户角色关系数据访问 Mapper
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 查询用户已关联的角色 ID，按角色 ID 升序返回
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") long userId);

    /**
     * 删除指定用户的全部角色关系
     */
    int deleteByUserId(@Param("userId") long userId);

    /**
     * 批量创建指定用户的角色关联
     */
    int insertRelations(@Param("userId") long userId, @Param("roleIds") Collection<Long> roleIds);
}
