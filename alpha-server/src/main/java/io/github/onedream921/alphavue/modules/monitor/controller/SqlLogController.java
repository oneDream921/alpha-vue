package io.github.onedream921.alphavue.modules.monitor.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogQuery;
import io.github.onedream921.alphavue.modules.monitor.dto.SqlLogSettingsRequest;
import io.github.onedream921.alphavue.modules.monitor.service.SqlLogService;
import io.github.onedream921.alphavue.modules.monitor.vo.SqlLogEntryVo;
import io.github.onedream921.alphavue.modules.monitor.vo.SqlLogSettingsVo;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SQL 监控接口。
 */
@Validated
@Tag(name = "SQL 日志")
@ApiSupport(order = 41, author = "Alpha Vue")
@RestController
@RequestMapping("/api/monitor/sql")
public class SqlLogController extends BaseController {

    private final SqlLogService sqlLogService;
    private final SystemAccessService access;

    public SqlLogController(SqlLogService sqlLogService, SystemAccessService access) {
        this.sqlLogService = sqlLogService;
        this.access = access;
    }

    /**
     * 查询最近 SQL 执行摘要。
     */
    @Operation(summary = "查询最近 SQL 日志")
    @GetMapping("/logs")
    public ApiResponse<List<SqlLogEntryVo>> logs(@RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit,
                                                 @RequestParam(required = false)
                                                 @Pattern(regexp = "SELECT|INSERT|UPDATE|DELETE|UNKNOWN",
                                                         flags = Pattern.Flag.CASE_INSENSITIVE) String type,
                                                 @RequestParam(required = false) @Size(max = 128) String keyword,
                                                 @RequestParam(defaultValue = "false") boolean slowOnly,
                                                 HttpServletRequest request) {
        access.require("monitor:sql:list");
        return success(sqlLogService.recent(new SqlLogQuery(limit, type, keyword, slowOnly)), request);
    }

    /**
     * 清空内存中的最近 SQL 日志。
     */
    @Operation(summary = "清空最近 SQL 日志")
    @DeleteMapping("/logs")
    public ApiResponse<String> clear(HttpServletRequest request) {
        access.require("monitor:sql:clear");
        sqlLogService.clear();
        return success("SQL 日志已清空", request);
    }

    /**
     * 查询 SQL 日志采集设置。
     */
    @Operation(summary = "查询 SQL 日志采集设置")
    @GetMapping("/settings")
    public ApiResponse<SqlLogSettingsVo> settings(HttpServletRequest request) {
        access.require("monitor:sql:list");
        return success(sqlLogService.settings(), request);
    }

    /**
     * 更新 SQL 日志采集设置。
     */
    @Operation(summary = "更新 SQL 日志采集设置")
    @PutMapping("/settings")
    public ApiResponse<SqlLogSettingsVo> updateSettings(@RequestBody @Valid SqlLogSettingsRequest body,
                                                        HttpServletRequest request) {
        access.require("monitor:sql:control");
        return success(sqlLogService.updateSettings(body), request);
    }

}
