package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class RoleRequests {
    private RoleRequests() { }

    public record Create(@NotBlank @Size(max = 64) String name,
                         @NotBlank @Size(max = 64) String code,
                         Integer sortOrder,
                         Integer status,
                         @Size(max = 500) String remark) { }

    public record Update(@NotBlank @Size(max = 64) String name,
                         Integer sortOrder,
                         Integer status,
                         @Size(max = 500) String remark) { }

    public record Assignment(Set<Long> menuIds) {
        public Assignment {
            menuIds = menuIds == null ? Set.of() : Set.copyOf(menuIds);
        }
    }
}
