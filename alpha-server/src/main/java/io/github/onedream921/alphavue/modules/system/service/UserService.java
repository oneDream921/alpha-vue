package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.UserRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysRole;
import io.github.onedream921.alphavue.modules.system.entity.SysUser;
import io.github.onedream921.alphavue.modules.system.mapper.SysRoleMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysDeptMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserRoleMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.vo.UserVo;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 用户业务服务
 */
@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private static final String ADMIN_USERNAME = "admin";

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;

    public UserService(SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper, SysDeptMapper deptMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.deptMapper = deptMapper;
    }

    /**
     * 分页查询用户，并附带用户已关联的角色 ID
     */
    public PageResponse<UserVo> page(int pageNumber, int pageSize) {
        Page<SysUser> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
        return new PageResponse<>(page.getRecords().stream().map(this::view).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询用户详情，不存在时返回统一请求错误
     */
    public UserVo get(long id) {
        return view(requireUser(id));
    }

    /**
     * 校验用户不是内置超级管理员
     */
    public void assertMutable(long id) {
        rejectAdminMutation(requireUser(id));
    }

    /**
     * 创建用户并加密保存初始密码
     */
    @Transactional
    public UserVo create(UserRequests.Create request) {
        if (baseMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())) > 0) {
            throw invalidRequest();
        }
        requireEnabledDept(request.deptId());
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        user.setNickname(request.nickname());
        user.setAvatar(request.avatar());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setDeptId(request.deptId());
        user.setStatus(1);
        user.setMustChangePassword(1);
        save(user);
        return view(user);
    }

    /**
     * 更新用户资料和启停状态
     */
    @Transactional
    public UserVo update(long id, UserRequests.Update request) {
        SysUser user = requireUser(id);
        rejectAdminMutation(user);
        requireEnabledDept(request.deptId());
        user.setNickname(request.nickname());
        user.setAvatar(request.avatar());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setDeptId(request.deptId());
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        updateById(user);
        return view(user);
    }

    /**
     * 软删除指定用户
     */
    @Transactional
    public void delete(long id) {
        rejectAdminMutation(requireUser(id));
        if (baseMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
    }

    /**
     * 管理员重置其他用户密码，并要求其下次登录后修改密码
     */
    @Transactional
    public void resetPassword(long id, long operatorId, String newPassword) {
        if (id == operatorId) {
            throw invalidRequest();
        }
        SysUser user = requireUser(id);
        rejectAdminMutation(user);
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setMustChangePassword(1);
        updateById(user);
    }

    /**
     * 替换用户角色关系，并校验目标角色均可用
     */
    @Transactional
    public void replaceRoles(long userId, Set<Long> roleIds) {
        SysUser user = requireUser(userId);
        rejectAdminMutation(user);
        if (!roleIds.isEmpty() && roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getDeleted, 0)) != roleIds.size()) {
            throw invalidRequest();
        }
        userRoleMapper.deleteByUserId(userId);
        if (!roleIds.isEmpty()) {
            userRoleMapper.insertRelations(userId, roleIds);
        }
    }

    private SysUser requireUser(long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw invalidRequest();
        }
        return user;
    }

    private static void rejectAdminMutation(SysUser user) {
        if (ADMIN_USERNAME.equals(user.getUsername())) {
            throw invalidRequest();
        }
    }

    private UserVo view(SysUser user) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        return UserVo.from(user, roleIds);
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }

    private void requireEnabledDept(Long deptId) {
        if (deptId != null && deptMapper.countEnabledById(deptId) == 0) {
            throw invalidRequest();
        }
    }
}
