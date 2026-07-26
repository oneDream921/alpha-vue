package io.github.onedream921.alphavue.modules.system.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.UserRequests;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.service.UserService;
import io.github.onedream921.alphavue.modules.system.vo.UserVo;
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
 * 用户管理接口
 */
@Validated
@Tag(name = "用户管理")
@ApiSupport(order = 20, author = "Alpha Vue")
@RestController
@RequestMapping("/api/system/users")
public class UserController {
    private final UserService userService;
    private final SystemAccessService access;

    public UserController(UserService userService, SystemAccessService access) {
        this.userService = userService;
        this.access = access;
    }

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户")
    @GetMapping
    public ApiResponse<PageResponse<UserVo>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        access.require("system:user:list");
        return ApiResponse.success(userService.page(page, size), traceId(request));
    }

    /**
     * 查询单个用户详情
     */
    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public ApiResponse<UserVo> get(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:user:list");
        return ApiResponse.success(userService.get(id), traceId(request));
    }

    /**
     * 新增用户并写入初始密码
     */
    @Operation(summary = "创建用户")
    @PostMapping
    @OperationLog(module = "System", operation = "Create user", type = BusinessType.CREATE)
    public ApiResponse<UserVo> create(@Valid @RequestBody UserRequests.Create body,
                                      HttpServletRequest request) {
        access.require("system:user:create");
        return ApiResponse.success(userService.create(body), traceId(request));
    }

    /**
     * 更新用户基础信息和启停状态
     */
    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update user", type = BusinessType.UPDATE)
    public ApiResponse<UserVo> update(@PathVariable @Positive long id, @Valid @RequestBody UserRequests.Update body,
                                      HttpServletRequest request) {
        access.require("system:user:update");
        return ApiResponse.success(userService.update(id, body), traceId(request));
    }

    /**
     * 软删除用户
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete user", type = BusinessType.DELETE)
    public ApiResponse<Void> delete(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:user:delete");
        userService.delete(id);
        return ApiResponse.success(null, traceId(request));
    }

    /**
     * 替换用户关联的角色集合
     */
    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    @OperationLog(module = "System", operation = "Assign user roles", type = BusinessType.GRANT)
    public ApiResponse<Void> replaceRoles(@PathVariable @Positive long id, @Valid @RequestBody UserRequests.RoleAssignment body,
                                          HttpServletRequest request) {
        access.require("system:role:assign");
        userService.replaceRoles(id, body.roleIds());
        return ApiResponse.success(null, traceId(request));
    }

    /**
     * 强制指定用户重新登录
     */
    @Operation(summary = "强制用户下线")
    @PutMapping("/{id}/kickout")
    @OperationLog(module = "System", operation = "Kick out user", type = BusinessType.FORCE)
    public ApiResponse<Void> kickout(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:user:update");
        userService.assertMutable(id);
        StpUtil.kickout(id);
        return ApiResponse.success(null, traceId(request));
    }

    /**
     * 管理员重置其他用户密码，并立即使目标用户会话失效
     */
    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    @OperationLog(module = "System", operation = "Reset user password", type = BusinessType.UPDATE)
    public ApiResponse<Void> resetPassword(@PathVariable @Positive long id,
                                           @Valid @RequestBody UserRequests.ResetPassword body,
                                           HttpServletRequest request) {
        access.require("system:user:reset-password");
        long operatorId = Long.parseLong(StpUtil.getLoginIdAsString());
        userService.resetPassword(id, operatorId, body.newPassword());
        StpUtil.kickout(id);
        return ApiResponse.success(null, traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
