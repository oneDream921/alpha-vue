package io.github.onedream921.alphavue.modules.system.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.MenuRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysMenu;
import io.github.onedream921.alphavue.modules.system.service.MenuService;
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
@RequestMapping("/api/system/menus")
public class MenuController {
    private final MenuService menuService;
    private final SystemAccessService access;

    public MenuController(MenuService menuService, SystemAccessService access) {
        this.menuService = menuService;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<PageResponse<SysMenu>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                    HttpServletRequest request) {
        access.require("system:menu:list");
        return ApiResponse.success(menuService.page(page, size), traceId(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysMenu> get(@PathVariable long id, HttpServletRequest request) {
        access.require("system:menu:list");
        return ApiResponse.success(menuService.get(id), traceId(request));
    }

    @PostMapping
    @OperationLog(module = "System", operation = "Create menu")
    public ApiResponse<SysMenu> create(@Valid @RequestBody MenuRequests.Save body, HttpServletRequest request) {
        access.require("system:menu:create");
        return ApiResponse.success(menuService.create(body), traceId(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update menu")
    public ApiResponse<SysMenu> update(@PathVariable long id, @Valid @RequestBody MenuRequests.Save body,
                                       HttpServletRequest request) {
        access.require("system:menu:update");
        return ApiResponse.success(menuService.update(id, body), traceId(request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete menu")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        access.require("system:menu:delete");
        menuService.delete(id);
        return ApiResponse.success(null, traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
