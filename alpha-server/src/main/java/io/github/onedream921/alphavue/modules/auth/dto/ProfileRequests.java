package io.github.onedream921.alphavue.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 个人资料请求对象
 */
public final class ProfileRequests {
    private ProfileRequests() { }

    /**
     * 个人资料更新请求
     */
    public record Update(@NotBlank @Size(max = 64) String nickname,
                         @Size(max = 255) String avatar,
                         @Email @Size(max = 128) String email,
                         @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
                         @Size(max = 32) String phone) { }

    /**
     * 当前用户修改密码请求
     */
    public record ChangePassword(@NotBlank @Size(max = 128) String currentPassword,
                                 @NotBlank @Size(min = 8, max = 100) String newPassword) { }
}
