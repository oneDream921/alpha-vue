package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 数据字典接口请求对象集合
 */
public final class DictRequests {
    private DictRequests() {
    }

    /**
     * 字典类型新增或更新请求
     */
    public record TypeSave(@NotBlank @Size(max = 64)
                           @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]*") String typeCode,
                           @NotBlank @Size(max = 64) String typeName,
                           @Min(0) @Max(1) Integer status,
                           @Size(max = 500) String remark) {
    }

    /**
     * 字典项新增或更新请求
     */
    public record ItemSave(@NotBlank @Size(max = 64) String label,
                           @NotBlank @Size(max = 128) String value,
                           @PositiveOrZero Integer sortOrder,
                           @Min(0) @Max(1) Integer status,
                           @Min(0) @Max(1) Integer isDefault,
                           @Size(max = 500) String remark) {
    }
}
