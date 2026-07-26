package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 菜单管理接口的请求对象集合
 */
public final class MenuRequests {
    private MenuRequests() { }

    /**
     * 新增或更新菜单请求
     */
    public record Save(@PositiveOrZero Long parentId,
                       @NotBlank @Size(max = 64) String title,
                       @NotBlank @Pattern(regexp = "MENU|BUTTON|DIRECTORY") String menuType,
                       @Size(max = 128) String path,
                       @Size(max = 255) String component,
                       @Size(max = 128) String permission,
                       @Size(max = 64) String icon,
                       @PositiveOrZero Integer sortOrder,
                       @Min(0) @Max(1) Integer visible,
                       @Min(0) @Max(1) Integer status) { }
}
