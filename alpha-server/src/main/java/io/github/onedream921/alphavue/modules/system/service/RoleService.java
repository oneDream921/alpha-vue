package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.RoleRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysMenu;
import io.github.onedream921.alphavue.modules.system.entity.SysRole;
import io.github.onedream921.alphavue.modules.system.mapper.SysMenuMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysRoleMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class RoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysMenuMapper menuMapper;
    private final JdbcTemplate jdbcTemplate;

    public RoleService(SysMenuMapper menuMapper, JdbcTemplate jdbcTemplate) {
        this.menuMapper = menuMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<SysRole> page(int pageNumber, int pageSize) {
        Page<SysRole> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder).orderByAsc(SysRole::getId));
        return new PageResponse<>(page.getRecords(), page.getTotal(), pageNumber, pageSize);
    }

    public SysRole get(long id) {
        return requireRole(id);
    }

    @Transactional
    public SysRole create(RoleRequests.Create request) {
        if (baseMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, request.code())) > 0) {
            throw invalidRequest();
        }
        SysRole role = new SysRole();
        role.setName(request.name());
        role.setCode(request.code());
        role.setSortOrder(defaultValue(request.sortOrder(), 0));
        role.setStatus(defaultValue(request.status(), 1));
        role.setRemark(request.remark());
        save(role);
        return role;
    }

    @Transactional
    public SysRole update(long id, RoleRequests.Update request) {
        SysRole role = requireRole(id);
        role.setName(request.name());
        role.setSortOrder(defaultValue(request.sortOrder(), role.getSortOrder()));
        role.setStatus(defaultValue(request.status(), role.getStatus()));
        role.setRemark(request.remark());
        updateById(role);
        return role;
    }

    @Transactional
    public void delete(long id) {
        SysRole role = requireRole(id);
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw invalidRequest();
        }
        removeById(id);
    }

    @Transactional
    public void replaceMenus(long roleId, Set<Long> menuIds) {
        requireRole(roleId);
        if (!menuIds.isEmpty() && menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)) != menuIds.size()) {
            throw invalidRequest();
        }
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        for (Long menuId : menuIds) {
            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        }
    }

    private SysRole requireRole(long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw invalidRequest();
        }
        return role;
    }

    private static int defaultValue(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}
