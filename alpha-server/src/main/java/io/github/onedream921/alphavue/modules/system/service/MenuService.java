package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.MenuRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysMenu;
import io.github.onedream921.alphavue.modules.system.mapper.SysMenuMapper;
import io.github.onedream921.alphavue.modules.system.vo.MenuVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单业务服务
 */
@Service
public class MenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    /**
     * 按父级和排序号分页查询菜单
     */
    public PageResponse<MenuVo> page(int pageNumber, int pageSize) {
        Page<SysMenu> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getSortOrder).orderByAsc(SysMenu::getId));
        return new PageResponse<>(page.getRecords().stream().map(MenuVo::from).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询角色可分配的启用菜单
     */
    public List<MenuVo> assignableMenus() {
        return list(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getDeleted, 0).orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSortOrder).orderByAsc(SysMenu::getId))
                .stream().map(MenuVo::from).toList();
    }

    /**
     * 查询菜单详情，不存在时返回统一请求错误
     */
    public MenuVo get(long id) {
        return MenuVo.from(requireMenu(id));
    }

    /**
     * 创建菜单并校验父菜单有效性
     */
    @Transactional
    public MenuVo create(MenuRequests.Save request) {
        validateParent(request.parentId(), null);
        SysMenu menu = new SysMenu();
        copy(request, menu);
        save(menu);
        return MenuVo.from(menu);
    }

    /**
     * 更新菜单并阻止将自身设为父级
     */
    @Transactional
    public MenuVo update(long id, MenuRequests.Save request) {
        SysMenu menu = requireMenu(id);
        validateParent(request.parentId(), id);
        copy(request, menu);
        updateById(menu);
        return MenuVo.from(menu);
    }

    /**
     * 删除菜单，存在子菜单时拒绝删除
     */
    @Transactional
    public void delete(long id) {
        requireMenu(id);
        if (baseMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0) {
            throw invalidRequest();
        }
        removeById(id);
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(selfId) || getById(parentId) == null) {
            throw invalidRequest();
        }
    }

    private static void copy(MenuRequests.Save request, SysMenu menu) {
        menu.setParentId(request.parentId() == null ? 0 : request.parentId());
        menu.setTitle(request.title());
        menu.setMenuType(request.menuType());
        menu.setPath(request.path());
        menu.setComponent(request.component());
        menu.setPermission(request.permission());
        menu.setIcon(request.icon());
        menu.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        menu.setVisible(request.visible() == null ? 1 : request.visible());
        menu.setStatus(request.status() == null ? 1 : request.status());
    }

    private SysMenu requireMenu(long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            throw invalidRequest();
        }
        return menu;
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}
