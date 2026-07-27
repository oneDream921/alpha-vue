package io.github.onedream921.alphavue.modules.system.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.RoleRequests;
import io.github.onedream921.alphavue.modules.system.service.RoleService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.vo.RoleVo;
import io.swagger.v3.oas.annotations.Operation;
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
 * 角色管理接口
 */
@Validated
@Tag(name = "角色管理")
@ApiSupport(order = 21, author = "Alpha Vue")
@RestController
@RequestMapping("/api/system/roles")
public class RoleController extends BaseController {
    private final RoleService roleService;
    private final SystemAccessService access;

    public RoleController(RoleService roleService, SystemAccessService access) {
        this.roleService = roleService;
        this.access = access;
    }

    /**
     * 分页查询角色列表
     */
    @Operation(summary = "分页查询角色")
    @GetMapping
    public ApiResponse<PageResponse<RoleVo>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                  HttpServletRequest request) {
        access.require("system:role:list");
        return success(roleService.page(page, size), request);
    }

    /**
     * 查询单个角色详情
     */
    @Operation(summary = "查询角色详情")
    @GetMapping("/{id}")
    public ApiResponse<RoleVo> get(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:role:list");
        return success(roleService.get(id), request);
    }

    /**
     * 新增角色
     */
    @Operation(summary = "创建角色")
    @PostMapping
    @OperationLog(module = "System", operation = "Create role", type = BusinessType.CREATE)
    public ApiResponse<RoleVo> create(@Valid @RequestBody RoleRequests.Create body, HttpServletRequest request) {
        access.require("system:role:create");
        return success(roleService.create(body), request);
    }

    /**
     * 更新角色基础信息
     */
    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update role", type = BusinessType.UPDATE)
    public ApiResponse<RoleVo> update(@PathVariable @Positive long id, @Valid @RequestBody RoleRequests.Update body,
                                      HttpServletRequest request) {
        access.require("system:role:update");
        return success(roleService.update(id, body), request);
    }

    /**
     * 删除非内置超级管理员角色
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete role", type = BusinessType.DELETE)
    public ApiResponse<Void> delete(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:role:delete");
        roleService.delete(id);
        return success(request);
    }

    /**
     * 替换角色关联的菜单集合
     */
    @Operation(summary = "分配角色菜单")
    @PutMapping("/{id}/menus")
    @OperationLog(module = "System", operation = "Assign role menus", type = BusinessType.GRANT)
    public ApiResponse<Void> replaceMenus(@PathVariable @Positive long id, @Valid @RequestBody RoleRequests.Assignment body,
                                          HttpServletRequest request) {
        access.require("system:role:assign");
        roleService.replaceMenus(id, body.menuIds());
        return success(request);
    }

    /**
     * 查询角色已关联的菜单 ID 集合
     */
    @Operation(summary = "查询角色菜单")
    @GetMapping("/{id}/menus")
    public ApiResponse<List<Long>> menuIds(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:role:list");
        return success(roleService.menuIds(id), request);
    }
}
