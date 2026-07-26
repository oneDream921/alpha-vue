package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 用户管理接口的请求对象集合
 */
public final class UserRequests {
    private UserRequests() { }

    /**
     * 新增用户请求
     */
    public record Create(@NotBlank @Size(max = 64) String username,
                         @NotBlank @Size(min = 8, max = 100) String password,
                         @NotBlank @Size(max = 64) String nickname,
                         @Size(max = 255) String avatar,
                         @Email @Size(max = 128) String email,
                         @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") @Size(max = 32) String phone,
                         @Positive Long deptId) { }

    /**
     * 更新用户资料和状态请求
     */
    public record Update(@NotBlank @Size(max = 64) String nickname,
                         @Size(max = 255) String avatar,
                         @Email @Size(max = 128) String email,
                         @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") @Size(max = 32) String phone,
                         @Positive Long deptId,
                         @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) Integer status) { }

    /**
     * 用户角色授权请求
     */
    public record RoleAssignment(Set<@Positive Long> roleIds) {
        public RoleAssignment {
            roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        }
    }

    /**
     * 管理员重置其他用户密码请求
     */
    public record ResetPassword(@NotBlank @Size(min = 8, max = 100) String newPassword) {
    }
}
