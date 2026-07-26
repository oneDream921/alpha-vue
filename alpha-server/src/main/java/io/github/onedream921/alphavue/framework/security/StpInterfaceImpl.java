package io.github.onedream921.alphavue.framework.security;

import cn.dev33.satoken.stp.StpInterface;
import io.github.onedream921.alphavue.modules.system.entity.SysUser;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限接口实现
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper userMapper;

    public StpInterfaceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 查询 Sa-Token 权限列表，超级管理员返回通配权限
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SysUser user = userMapper.selectActiveById(Long.parseLong(loginId.toString()));
        if (user != null && ADMIN_USERNAME.equals(user.getUsername())) {
            return List.of("*");
        }
        if (getRoleList(loginId, loginType).contains("SUPER_ADMIN")) {
            return List.of("*");
        }
        return userMapper.selectPermissionCodesByUserId(loginId);
    }

    /**
     * 查询 Sa-Token 角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return userMapper.selectRoleCodesByUserId(loginId);
    }
}
