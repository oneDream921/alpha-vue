package io.github.onedream921.alphavue.modules.monitor.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.TraceIdFilter;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.monitor.dto.RedisKeyQuery;
import io.github.onedream921.alphavue.modules.monitor.service.RedisManagementService;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyMetadataVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisKeyPageVo;
import io.github.onedream921.alphavue.modules.monitor.vo.RedisOverviewVo;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 运维接口
 */
@Validated
@Tag(name = "Redis 管理")
@ApiSupport(order = 40, author = "Alpha Vue")
@RestController
@RequestMapping("/api/monitor/redis")
public class RedisManagementController {
    private final RedisManagementService redisManagementService;
    private final SystemAccessService access;

    public RedisManagementController(RedisManagementService redisManagementService, SystemAccessService access) {
        this.redisManagementService = redisManagementService;
        this.access = access;
    }

    /**
     * 查询 Redis 概览
     */
    @Operation(summary = "查询 Redis 概览")
    @GetMapping("/overview")
    public ApiResponse<RedisOverviewVo> overview(HttpServletRequest request) {
        access.require("monitor:redis:list");
        return ApiResponse.success(redisManagementService.overview(), traceId(request));
    }

    /**
     * 使用游标查询 Redis 键
     */
    @Operation(summary = "分页查询 Redis 键")
    @GetMapping("/keys")
    public ApiResponse<RedisKeyPageVo> keys(@RequestParam(defaultValue = "") @Size(max = 128) String prefix,
                                             @RequestParam(defaultValue = "0") @Pattern(regexp = "\\d+") String cursor,
                                             @RequestParam(defaultValue = "50") @Min(1) @Max(100) int count,
                                             @RequestParam(required = false) @Size(max = 128) String keyword,
                                             HttpServletRequest request) {
        access.require("monitor:redis:list");
        return ApiResponse.success(redisManagementService.page(new RedisKeyQuery(prefix, cursor, count, keyword)),
                traceId(request));
    }

    /**
     * 查询 Redis 键元数据和值预览
     */
    @Operation(summary = "查询 Redis 键元数据")
    @GetMapping("/key")
    public ApiResponse<RedisKeyMetadataVo> key(@RequestParam @NotBlank @Size(max = 512) String key,
                                                HttpServletRequest request) {
        access.require("monitor:redis:list");
        return ApiResponse.success(redisManagementService.metadata(key), traceId(request));
    }

    /**
     * 删除单个 Redis 键
     */
    @Operation(summary = "删除 Redis 键")
    @DeleteMapping("/key")
    @OperationLog(module = "Monitor", operation = "删除 Redis 键", type = BusinessType.DELETE,
            saveRequest = false, saveResponse = false)
    public ApiResponse<String> delete(@RequestParam @NotBlank @Size(max = 512) String key,
                                      HttpServletRequest request) {
        access.require("monitor:redis:delete");
        redisManagementService.delete(key);
        return ApiResponse.success("Redis 键已删除", traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
