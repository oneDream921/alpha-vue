package io.github.onedream921.alphavue.modules.system.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.DeptRequests;
import io.github.onedream921.alphavue.modules.system.service.DeptService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.vo.DeptVo;
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

/**
 * 部门管理接口
 */
@Validated
@Tag(name = "部门管理")
@ApiSupport(order = 23, author = "Alpha Vue")
@RestController
@RequestMapping("/api/system/depts")
public class DeptController {
    private final DeptService deptService;
    private final SystemAccessService access;

    public DeptController(DeptService deptService, SystemAccessService access) {
        this.deptService = deptService;
        this.access = access;
    }

    /**
     * 分页查询部门列表
     */
    @Operation(summary = "分页查询部门")
    @GetMapping
    public ApiResponse<PageResponse<DeptVo>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                  HttpServletRequest request) {
        access.require("system:dept:list");
        return ApiResponse.success(deptService.page(page, size), traceId(request));
    }

    /**
     * 查询单个部门详情
     */
    @Operation(summary = "查询部门详情")
    @GetMapping("/{id}")
    public ApiResponse<DeptVo> get(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dept:list");
        return ApiResponse.success(deptService.get(id), traceId(request));
    }

    /**
     * 新增部门
     */
    @Operation(summary = "创建部门")
    @PostMapping
    @OperationLog(module = "System", operation = "Create department", type = BusinessType.CREATE)
    public ApiResponse<DeptVo> create(@Valid @RequestBody DeptRequests.Save body, HttpServletRequest request) {
        access.require("system:dept:create");
        return ApiResponse.success(deptService.create(body), traceId(request));
    }

    /**
     * 更新部门基础信息
     */
    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update department", type = BusinessType.UPDATE)
    public ApiResponse<DeptVo> update(@PathVariable @Positive long id, @Valid @RequestBody DeptRequests.Save body,
                                      HttpServletRequest request) {
        access.require("system:dept:update");
        return ApiResponse.success(deptService.update(id, body), traceId(request));
    }

    /**
     * 删除没有子部门的部门
     */
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete department", type = BusinessType.DELETE)
    public ApiResponse<Void> delete(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dept:delete");
        deptService.delete(id);
        return ApiResponse.success(null, traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
