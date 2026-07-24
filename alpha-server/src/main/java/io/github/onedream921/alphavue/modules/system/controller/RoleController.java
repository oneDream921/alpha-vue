package io.github.onedream921.alphavue.modules.system.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.RoleRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysRole;
import io.github.onedream921.alphavue.modules.system.service.RoleService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@Validated
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {
    private final RoleService roleService;
    private final SystemAccessService access;

    public RoleController(RoleService roleService, SystemAccessService access) {
        this.roleService = roleService;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<PageResponse<SysRole>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                    HttpServletRequest request) {
        access.require("system:role:list");
        return ApiResponse.success(roleService.page(page, size), traceId(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysRole> get(@PathVariable long id, HttpServletRequest request) {
        access.require("system:role:list");
        return ApiResponse.success(roleService.get(id), traceId(request));
    }

    @PostMapping
    @OperationLog(module = "System", operation = "Create role")
    public ApiResponse<SysRole> create(@Valid @RequestBody RoleRequests.Create body, HttpServletRequest request) {
        access.require("system:role:create");
        return ApiResponse.success(roleService.create(body), traceId(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update role")
    public ApiResponse<SysRole> update(@PathVariable long id, @Valid @RequestBody RoleRequests.Update body,
                                       HttpServletRequest request) {
        access.require("system:role:update");
        return ApiResponse.success(roleService.update(id, body), traceId(request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete role")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        access.require("system:role:delete");
        roleService.delete(id);
        return ApiResponse.success(null, traceId(request));
    }

    @PutMapping("/{id}/menus")
    @OperationLog(module = "System", operation = "Assign role menus")
    public ApiResponse<Void> replaceMenus(@PathVariable long id, @RequestBody RoleRequests.Assignment body,
                                          HttpServletRequest request) {
        access.require("system:role:assign");
        roleService.replaceMenus(id, body.menuIds());
        return ApiResponse.success(null, traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
