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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    public PageResponse<SysMenu> page(int pageNumber, int pageSize) {
        Page<SysMenu> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getSortOrder).orderByAsc(SysMenu::getId));
        return new PageResponse<>(page.getRecords(), page.getTotal(), pageNumber, pageSize);
    }

    public SysMenu get(long id) {
        return requireMenu(id);
    }

    @Transactional
    public SysMenu create(MenuRequests.Save request) {
        validateParent(request.parentId(), null);
        SysMenu menu = new SysMenu();
        copy(request, menu);
        save(menu);
        return menu;
    }

    @Transactional
    public SysMenu update(long id, MenuRequests.Save request) {
        SysMenu menu = requireMenu(id);
        validateParent(request.parentId(), id);
        copy(request, menu);
        updateById(menu);
        return menu;
    }

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
