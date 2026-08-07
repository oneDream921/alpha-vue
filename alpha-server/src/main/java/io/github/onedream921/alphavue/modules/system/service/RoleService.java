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
import io.github.onedream921.alphavue.modules.system.mapper.SysRoleMenuMapper;
import io.github.onedream921.alphavue.modules.system.mapper.SysRoleMapper;
import io.github.onedream921.alphavue.modules.system.vo.RoleVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色业务服务
 */
@Service
public class RoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public RoleService(SysMenuMapper menuMapper, SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /**
     * 按排序号分页查询角色
     */
    public PageResponse<RoleVo> page(int pageNumber, int pageSize) {
        Page<SysRole> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder).orderByAsc(SysRole::getId));
        return new PageResponse<>(page.getRecords().stream().map(RoleVo::from).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询角色详情，不存在时返回统一请求错误
     */
    public RoleVo get(long id) {
        return RoleVo.from(requireRole(id));
    }

    /**
     * 创建角色并校验角色编码唯一性
     */
    @Transactional
    public RoleVo create(RoleRequests.Create request) {
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
        return RoleVo.from(role);
    }

    /**
     * 更新角色名称、排序、状态和备注
     */
    @Transactional
    public RoleVo update(long id, RoleRequests.Update request) {
        SysRole role = requireRole(id);
        role.setName(request.name());
        role.setSortOrder(defaultValue(request.sortOrder(), role.getSortOrder()));
        role.setStatus(defaultValue(request.status(), role.getStatus()));
        role.setRemark(request.remark());
        updateById(role);
        return RoleVo.from(role);
    }

    /**
     * 删除角色，内置超级管理员角色不允许删除
     */
    @Transactional
    public void delete(long id) {
        SysRole role = requireRole(id);
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw invalidRequest();
        }
        if (baseMapper.softDeleteById(id) != 1) {
            throw invalidRequest();
        }
    }

    /**
     * 替换角色菜单关系，并校验目标菜单均可用
     */
    @Transactional
    public void replaceMenus(long roleId, Set<Long> menuIds) {
        requireRole(roleId);
        if (!menuIds.isEmpty() && menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getDeleted, 0)) != menuIds.size()) {
            throw invalidRequest();
        }
        validateMenuHierarchy(menuIds);
        roleMenuMapper.deleteByRoleId(roleId);
        if (!menuIds.isEmpty()) {
            roleMenuMapper.insertRelations(roleId, menuIds);
        }
    }

    /**
     * 查询角色已关联的菜单 ID 集合
     */
    public List<Long> menuIds(long roleId) {
        requireRole(roleId);
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 校验角色菜单关系完整，避免按钮权限脱离所属菜单。
     */
    private void validateMenuHierarchy(Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        Map<Long, SysMenu> menusById = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(SysMenu::getId, Function.identity()));
        for (SysMenu menu : menusById.values()) {
            Long parentId = menu.getParentId();
            if (parentId != null && parentId != 0 && !menuIds.contains(parentId)) {
                throw new BusinessException(400, PublicErrorMessage.MENU_PARENT_REQUIRED);
            }
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
