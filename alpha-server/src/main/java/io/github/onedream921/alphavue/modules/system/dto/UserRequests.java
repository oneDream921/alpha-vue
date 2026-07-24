package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class UserRequests {
    private UserRequests() { }

    public record Create(@NotBlank @Size(max = 64) String username,
                         @NotBlank @Size(min = 8, max = 100) String password,
                         @NotBlank @Size(max = 64) String nickname,
                         @Size(max = 255) String avatar,
                         @Email @Size(max = 128) String email,
                         @Size(max = 32) String phone,
                         Long deptId) { }

    public record Update(@NotBlank @Size(max = 64) String nickname,
                         @Size(max = 255) String avatar,
                         @Email @Size(max = 128) String email,
                         @Size(max = 32) String phone,
                         Long deptId,
                         Integer status) { }

    public record RoleAssignment(Set<Long> roleIds) {
        public RoleAssignment {
            roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        }
    }
}
