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
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final SysRoleMapper roleMapper;
    private final JdbcTemplate jdbcTemplate;

    public UserService(SysRoleMapper roleMapper, JdbcTemplate jdbcTemplate) {
        this.roleMapper = roleMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<UserView> page(int pageNumber, int pageSize) {
        Page<SysUser> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
        return new PageResponse<>(page.getRecords().stream().map(UserService::view).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    public UserView get(long id) {
        return view(requireUser(id));
    }

    @Transactional
    public UserView create(UserRequests.Create request) {
        if (baseMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())) > 0) {
            throw invalidRequest();
        }
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

    @Transactional
    public UserView update(long id, UserRequests.Update request) {
        SysUser user = requireUser(id);
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

    @Transactional
    public void delete(long id) {
        requireUser(id);
        removeById(id);
    }

    @Transactional
    public void replaceRoles(long userId, Set<Long> roleIds) {
        requireUser(userId);
        if (!roleIds.isEmpty() && roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)) != roleIds.size()) {
            throw invalidRequest();
        }
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (Long roleId : roleIds) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
    }

    private SysUser requireUser(long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw invalidRequest();
        }
        return user;
    }

    private static UserView view(SysUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getEmail(),
                user.getPhone(), user.getDeptId(), user.getStatus(), user.getMustChangePassword(), user.getCreatedAt());
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }

    public record UserView(Long id, String username, String nickname, String avatar, String email, String phone,
                           Long deptId, Integer status, Integer mustChangePassword, java.time.LocalDateTime createdAt) { }
}
