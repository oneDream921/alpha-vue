package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 部门管理接口的请求对象集合
 */
public final class DeptRequests {
    private DeptRequests() { }

    /**
     * 新增或更新部门请求
     */
    public record Save(@PositiveOrZero Long parentId,
                       @NotBlank @Size(max = 64) String name,
                       @PositiveOrZero Integer sortOrder,
                       @Min(0) @Max(1) Integer status) { }
}
