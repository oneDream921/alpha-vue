package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.system.entity.SysUser;
import io.github.onedream921.alphavue.modules.system.vo.UserSummaryVo;
import io.github.onedream921.alphavue.modules.system.vo.RouteVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 用户数据访问 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询启用且未删除的用户
     */
    SysUser selectActiveByUsername(@Param("username") String username);

    /**
     * 根据用户 ID 查询启用且未删除的用户
     */
    SysUser selectActiveById(@Param("id") long id);

    /**
     * 根据用户 ID 批量查询可展示的用户信息
     */
    List<UserSummaryVo> selectActiveSummariesByIds(@Param("ids") Collection<Long> ids);

    /**
     * 统计指定用户 ID 是否仍为启用且未删除状态
     */
    int countActiveById(@Param("id") Object id);

    /**
     * 更新当前启用用户的个人资料
     */
    int updateActiveProfile(@Param("id") long id, @Param("nickname") String nickname, @Param("avatar") String avatar,
                            @Param("email") String email, @Param("phone") String phone);

    /**
     * 更新当前启用用户的头像
     */
    int updateActiveAvatar(@Param("id") long id, @Param("avatar") String avatar);

    /**
     * 更新当前启用用户的密码并清除强制改密标记
     */
    int updateActivePassword(@Param("id") long id, @Param("password") String password);

    /**
     * 查询用户拥有的启用角色编码
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Object userId);

    /**
     * 查询用户拥有的启用菜单权限编码
     */
    List<String> selectPermissionCodesByUserId(@Param("userId") Object userId);

    /**
     * 查询用户可见的启用菜单路由
     */
    List<RouteVo> selectVisibleRoutesByUserId(@Param("userId") long userId);
}
