package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 角色管理接口的请求对象集合
 */
public final class RoleRequests {
    private RoleRequests() { }

    /**
     * 新增角色请求
     */
    public record Create(@NotBlank @Size(max = 64) String name,
                         @NotBlank @Size(max = 64) String code,
                         @PositiveOrZero Integer sortOrder,
                         @Min(0) @Max(1) Integer status,
                         @Size(max = 500) String remark) { }

    /**
     * 更新角色请求，角色编码不允许通过该接口修改
     */
    public record Update(@NotBlank @Size(max = 64) String name,
                         @PositiveOrZero Integer sortOrder,
                         @Min(0) @Max(1) Integer status,
                         @Size(max = 500) String remark) { }

    /**
     * 角色菜单授权请求
     */
    public record Assignment(Set<@Positive Long> menuIds) {
        public Assignment {
            menuIds = menuIds == null ? Set.of() : Set.copyOf(menuIds);
        }
    }
}
