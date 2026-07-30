package io.github.onedream921.alphavue.modules.system.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.ConfigRequests;
import io.github.onedream921.alphavue.modules.system.service.ConfigService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.vo.ConfigVo;
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
 * 参数配置管理接口
 */
@Validated
@Tag(name = "参数配置")
@RestController
@RequestMapping("/api/system/configs")
public class ConfigController extends BaseController {
    private final ConfigService configService;
    private final SystemAccessService access;

    public ConfigController(ConfigService configService, SystemAccessService access) {
        this.configService = configService;
        this.access = access;
    }

    /**
     * 分页查询参数配置
     */
    @GetMapping
    public ApiResponse<PageResponse<ConfigVo>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                    HttpServletRequest request) {
        access.require("system:config:list");
        return success(configService.page(page, size), request);
    }

    /**
     * 查询单个参数配置详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ConfigVo> get(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:config:list");
        return success(configService.get(id), request);
    }

    /**
     * 创建参数配置并立即发布到 Redis
     */
    @PostMapping
    @OperationLog(module = "System", operation = "Create configuration", type = BusinessType.CREATE)
    public ApiResponse<ConfigVo> create(@Valid @RequestBody ConfigRequests.Save body, HttpServletRequest request) {
        access.require("system:config:create");
        return success(configService.create(body), request);
    }

    /**
     * 更新参数配置并立即发布到 Redis
     */
    @PutMapping("/{id}")
    @OperationLog(module = "System", operation = "Update configuration", type = BusinessType.UPDATE)
    public ApiResponse<ConfigVo> update(@PathVariable @Positive long id, @Valid @RequestBody ConfigRequests.Save body,
                                        HttpServletRequest request) {
        access.require("system:config:update");
        return success(configService.update(id, body), request);
    }

    /**
     * 删除参数配置并移除 Redis 缓存
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "System", operation = "Delete configuration", type = BusinessType.DELETE)
    public ApiResponse<Void> delete(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:config:delete");
        configService.delete(id);
        return success(request);
    }
}
