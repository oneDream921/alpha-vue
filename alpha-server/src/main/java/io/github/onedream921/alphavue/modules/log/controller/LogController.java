package io.github.onedream921.alphavue.modules.log.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.log.service.LogQueryService;
import io.github.onedream921.alphavue.modules.log.dto.OperationLogQuery;
import io.github.onedream921.alphavue.modules.log.vo.LoginLogVo;
import io.github.onedream921.alphavue.modules.log.vo.OperationLogVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志查询接口
 */
@Validated
@Tag(name = "日志查询")
@ApiSupport(order = 50, author = "Alpha Vue")
@RestController
@RequestMapping("/api/logs")
public class LogController extends BaseController {

    private final LogQueryService logQueryService;
    private final SystemAccessService access;

    public LogController(LogQueryService logQueryService, SystemAccessService access) {
        this.logQueryService = logQueryService;
        this.access = access;
    }

    /**
     * 分页查询操作日志
     */
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/operations")
    public ApiResponse<PageResponse<OperationLogVo>> operations(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(required = false) @Min(0) @Max(2) Integer handlingStatus,
            @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 128) String keyword,
            HttpServletRequest request) {
        access.require("log:operation:list");
        return success(logQueryService.operations(page, size,
                new OperationLogQuery(keyword == null ? null : keyword.trim(), status, handlingStatus)), request);
    }

    /**
     * 分页查询登录日志
     */
    @Operation(summary = "分页查询登录日志")
    @GetMapping("/logins")
    public ApiResponse<PageResponse<LoginLogVo>> logins(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        access.require("log:login:list");
        return success(logQueryService.logins(page, size), request);
    }

    /**
     * 更新失败操作日志的处理状态：0 未处理、1 已处理、2 已忽略
     */
    @Operation(summary = "更新异常日志处理状态")
    @PutMapping("/operations/{id}/handled")
    @OperationLog(module = "Log", operation = "Update operation log handling status", type = BusinessType.UPDATE)
    public ApiResponse<Void> updateHandlingStatus(@PathVariable @Min(1) long id,
                                         @RequestParam @Min(0) @Max(2) int handlingStatus,
                                         HttpServletRequest request) {
        access.require("log:operation:handle");
        long operatorId = loginUserId();
        if (!logQueryService.updateHandlingStatus(id, handlingStatus, operatorId)) {
            throw new io.github.onedream921.alphavue.common.exception.BusinessException(400,
                    io.github.onedream921.alphavue.common.exception.PublicErrorMessage.INVALID_REQUEST);
        }
        return success(request);
    }
}
