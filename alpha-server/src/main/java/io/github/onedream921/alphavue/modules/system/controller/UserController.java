package io.github.onedream921.alphavue.modules.system.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.UserRequests;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.service.UserService;
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
@RequestMapping("/api/system/users")
public class UserController {
    private final UserService userService;
    private final SystemAccessService access;

    public UserController(UserService userService, SystemAccessService access) {
        this.userService = userService;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserService.UserView>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        access.require("system:user:list");
        return ApiResponse.success(userService.page(page, size), traceId(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserService.UserView> get(@PathVariable long id, HttpServletRequest request) {
        access.require("system:user:list");
        return ApiResponse.success(userService.get(id), traceId(request));
    }

    @PostMapping
    @OperationLog(module = "System", operation = "Create user")
    public ApiResponse<UserService.UserView> create(@Valid @RequestBody UserRequests.Create body,
                                                     HttpServletRequest request) {
        access.require("system:user:create");
        return ApiResponse.success(userService.create(body), traceId(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update user")
    public ApiResponse<UserService.UserView> update(@PathVariable long id, @Valid @RequestBody UserRequests.Update body,
                                                     HttpServletRequest request) {
        access.require("system:user:update");
        return ApiResponse.success(userService.update(id, body), traceId(request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete user")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        access.require("system:user:delete");
        userService.delete(id);
        return ApiResponse.success(null, traceId(request));
    }

    @PutMapping("/{id}/roles")
    @OperationLog(module = "System", operation = "Assign user roles")
    public ApiResponse<Void> replaceRoles(@PathVariable long id, @RequestBody UserRequests.RoleAssignment body,
                                          HttpServletRequest request) {
        access.require("system:role:assign");
        userService.replaceRoles(id, body.roleIds());
        return ApiResponse.success(null, traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
