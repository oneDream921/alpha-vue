package io.github.onedream921.alphavue.modules.system.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.MenuRequests;
import io.github.onedream921.alphavue.modules.system.service.MenuService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.vo.MenuVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口
 */
@Validated
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menus")
public class MenuController extends BaseController {
    private final MenuService menuService;
    private final SystemAccessService access;

    public MenuController(MenuService menuService, SystemAccessService access) {
        this.menuService = menuService;
        this.access = access;
    }

    /**
     * 分页查询菜单列表
     */
    @GetMapping
    public ApiResponse<PageResponse<MenuVo>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                  HttpServletRequest request) {
        access.require("system:menu:list");
        return success(menuService.page(page, size), request);
    }

    /**
     * 查询角色可分配菜单
     */
    @GetMapping("/assignable")
    public ApiResponse<List<MenuVo>> assignable(HttpServletRequest request) {
        access.require("system:role:assign");
        return success(menuService.assignableMenus(), request);
    }

    /**
     * 查询单个菜单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MenuVo> get(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:menu:list");
        return success(menuService.get(id), request);
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @OperationLog(module = "System", operation = "Create menu", type = BusinessType.CREATE)
    public ApiResponse<MenuVo> create(@Valid @RequestBody MenuRequests.Save body, HttpServletRequest request) {
        access.require("system:menu:create");
        return success(menuService.create(body), request);
    }

    /**
     * 更新菜单基础信息
     */
    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update menu", type = BusinessType.UPDATE)
    public ApiResponse<MenuVo> update(@PathVariable @Positive long id, @Valid @RequestBody MenuRequests.Save body,
                                      HttpServletRequest request) {
        access.require("system:menu:update");
        return success(menuService.update(id, body), request);
    }

    /**
     * 删除没有子菜单的菜单
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete menu", type = BusinessType.DELETE)
    public ApiResponse<Void> delete(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:menu:delete");
        menuService.delete(id);
        return success(request);
    }
}
