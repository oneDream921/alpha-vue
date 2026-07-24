package io.github.onedream921.alphavue.modules.system.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.DeptRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysDept;
import io.github.onedream921.alphavue.modules.system.service.DeptService;
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
@RequestMapping("/api/system/depts")
public class DeptController {
    private final DeptService deptService;
    private final SystemAccessService access;

    public DeptController(DeptService deptService, SystemAccessService access) {
        this.deptService = deptService;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<PageResponse<SysDept>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                    HttpServletRequest request) {
        access.require("system:dept:list");
        return ApiResponse.success(deptService.page(page, size), traceId(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysDept> get(@PathVariable long id, HttpServletRequest request) {
        access.require("system:dept:list");
        return ApiResponse.success(deptService.get(id), traceId(request));
    }

    @PostMapping
    @OperationLog(module = "System", operation = "Create department")
    public ApiResponse<SysDept> create(@Valid @RequestBody DeptRequests.Save body, HttpServletRequest request) {
        access.require("system:dept:create");
        return ApiResponse.success(deptService.create(body), traceId(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update department")
    public ApiResponse<SysDept> update(@PathVariable long id, @Valid @RequestBody DeptRequests.Save body,
                                       HttpServletRequest request) {
        access.require("system:dept:update");
        return ApiResponse.success(deptService.update(id, body), traceId(request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete department")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        access.require("system:dept:delete");
        deptService.delete(id);
        return ApiResponse.success(null, traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
