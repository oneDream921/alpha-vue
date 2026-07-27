package io.github.onedream921.alphavue.modules.system.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.dto.DictRequests;
import io.github.onedream921.alphavue.modules.system.service.DictService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.vo.DictItemVo;
import io.github.onedream921.alphavue.modules.system.vo.DictTypeVo;
import io.github.onedream921.alphavue.modules.system.vo.EnabledDictItemVo;
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

import java.util.List;

/**
 * 数据字典管理接口
 */
@Tag(name = "数据字典")
@ApiSupport(order = 25, author = "Alpha Vue")
@Validated
@RestController
@RequestMapping("/api/system")
public class DictController extends BaseController {
    private final DictService dictService;
    private final SystemAccessService access;

    public DictController(DictService dictService, SystemAccessService access) {
        this.dictService = dictService;
        this.access = access;
    }

    /**
     * 分页查询字典类型
     */
    @Operation(summary = "分页查询字典类型")
    @GetMapping("/dict-types")
    public ApiResponse<PageResponse<DictTypeVo>> pageTypes(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                            HttpServletRequest request) {
        access.require("system:dict:list");
        return success(dictService.pageTypes(page, size), request);
    }

    /**
     * 查询字典类型详情
     */
    @Operation(summary = "查询字典类型详情")
    @GetMapping("/dict-types/{id}")
    public ApiResponse<DictTypeVo> getType(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dict:list");
        return success(dictService.getType(id), request);
    }

    /**
     * 创建字典类型
     */
    @Operation(summary = "创建字典类型")
    @PostMapping("/dict-types")
    @OperationLog(module = "System", operation = "Create dictionary type", type = BusinessType.CREATE)
    public ApiResponse<DictTypeVo> createType(@Valid @RequestBody DictRequests.TypeSave body,
                                              HttpServletRequest request) {
        access.require("system:dict:create");
        return success(dictService.createType(body), request);
    }

    /**
     * 更新字典类型
     */
    @Operation(summary = "更新字典类型")
    @PutMapping("/dict-types/{id}")
    @OperationLog(module = "System", operation = "Update dictionary type", type = BusinessType.UPDATE)
    public ApiResponse<DictTypeVo> updateType(@PathVariable @Positive long id,
                                              @Valid @RequestBody DictRequests.TypeSave body,
                                              HttpServletRequest request) {
        access.require("system:dict:update");
        return success(dictService.updateType(id, body), request);
    }

    /**
     * 删除字典类型
     */
    @Operation(summary = "删除字典类型")
    @DeleteMapping("/dict-types/{id}")
    @OperationLog(module = "System", operation = "Delete dictionary type", type = BusinessType.DELETE)
    public ApiResponse<Void> deleteType(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dict:delete");
        dictService.deleteType(id);
        return success(request);
    }

    /**
     * 分页查询字典项
     */
    @Operation(summary = "分页查询字典项")
    @GetMapping("/dict-types/{typeId}/items")
    public ApiResponse<PageResponse<DictItemVo>> pageItems(@PathVariable @Positive long typeId,
                                                           @RequestParam(defaultValue = "1") @Min(1) int page,
                                                           @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                           HttpServletRequest request) {
        access.require("system:dict:list");
        return success(dictService.pageItems(typeId, page, size), request);
    }

    /**
     * 创建字典项
     */
    @Operation(summary = "创建字典项")
    @PostMapping("/dict-types/{typeId}/items")
    @OperationLog(module = "System", operation = "Create dictionary item", type = BusinessType.CREATE)
    public ApiResponse<DictItemVo> createItem(@PathVariable @Positive long typeId,
                                              @Valid @RequestBody DictRequests.ItemSave body,
                                              HttpServletRequest request) {
        access.require("system:dict:create");
        return success(dictService.createItem(typeId, body), request);
    }

    /**
     * 更新字典项
     */
    @Operation(summary = "更新字典项")
    @PutMapping("/dict-items/{id}")
    @OperationLog(module = "System", operation = "Update dictionary item", type = BusinessType.UPDATE)
    public ApiResponse<DictItemVo> updateItem(@PathVariable @Positive long id,
                                              @Valid @RequestBody DictRequests.ItemSave body,
                                              HttpServletRequest request) {
        access.require("system:dict:update");
        return success(dictService.updateItem(id, body), request);
    }

    /**
     * 删除字典项
     */
    @Operation(summary = "删除字典项")
    @DeleteMapping("/dict-items/{id}")
    @OperationLog(module = "System", operation = "Delete dictionary item", type = BusinessType.DELETE)
    public ApiResponse<Void> deleteItem(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dict:delete");
        dictService.deleteItem(id);
        return success(request);
    }

    /**
     * 查询指定类型的启用字典项
     */
    @Operation(summary = "查询启用字典项")
    @GetMapping("/dicts/{typeCode}/items")
    public ApiResponse<List<EnabledDictItemVo>> enabledItems(@PathVariable @jakarta.validation.constraints.Size(max = 64) String typeCode,
                                                              HttpServletRequest request) {
        return success(dictService.enabledItems(typeCode), request);
    }
}
