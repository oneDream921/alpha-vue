package io.github.onedream921.alphavue.modules.monitor.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.monitor.service.OnlineSessionService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/monitor/online-users")
public class OnlineSessionController extends BaseController {

    private final OnlineSessionService service;
    private final SystemAccessService access;

    public OnlineSessionController(OnlineSessionService service, SystemAccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<PageResponse<OnlineSessionService.OnlineSessionVo>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        access.require("monitor:online:list");
        return success(service.page(page, size), request);
    }

    @DeleteMapping("/{userId}/sessions/{terminalIndex}")
    @OperationLog(module = "Monitor", operation = "Kick online session", type = BusinessType.FORCE)
    public ApiResponse<Void> kickout(@PathVariable @Positive long userId,
                                     @PathVariable @Positive int terminalIndex,
                                     HttpServletRequest request) {
        access.require("monitor:online:kickout");
        service.kickout(userId, terminalIndex);
        return success(request);
    }
}
