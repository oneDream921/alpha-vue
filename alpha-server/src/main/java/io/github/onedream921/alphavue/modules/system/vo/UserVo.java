package io.github.onedream921.alphavue.modules.system.vo;

import io.github.onedream921.alphavue.modules.system.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户接口响应视图
 */
public record UserVo(Long id, String username, String nickname, String avatar, String email, String phone,
                     Long deptId, Integer status, Integer mustChangePassword, List<Long> roleIds,
                     LocalDateTime createdAt) {
    /**
     * 从用户实体和角色 ID 集合转换为响应视图
     */
    public static UserVo from(SysUser user, List<Long> roleIds) {
        return new UserVo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getEmail(),
                user.getPhone(), user.getDeptId(), user.getStatus(), user.getMustChangePassword(), roleIds,
                user.getCreatedAt());
    }
}
