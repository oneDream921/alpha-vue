package io.github.onedream921.alphavue.modules.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 角色菜单关系数据访问 Mapper
 */
@Mapper
public interface SysRoleMenuMapper {

    /**
     * 查询角色已关联的菜单 ID，按菜单 ID 升序返回
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") long roleId);

    /**
     * 删除指定角色的全部菜单关系
     */
    int deleteByRoleId(@Param("roleId") long roleId);

    /**
     * 批量创建指定角色的菜单关联
     */
    int insertRelations(@Param("roleId") long roleId, @Param("menuIds") Collection<Long> menuIds);
}
