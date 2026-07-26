package io.github.onedream921.alphavue.modules.log.vo;

import io.github.onedream921.alphavue.modules.log.entity.SysLoginLog;

import java.time.LocalDateTime;

/**
 * 登录日志接口响应视图
 */
public record LoginLogVo(long id, String username, Long userId, String loginType, int status,
                         String ipAddress, String userAgent, String message, LocalDateTime createdAt) {
    /**
     * 从登录日志实体转换为响应视图
     */
    public static LoginLogVo from(SysLoginLog log) {
        return new LoginLogVo(log.getId(), log.getUsername(), log.getUserId(), log.getLoginType(),
                log.getStatus(), log.getIpAddress(), log.getUserAgent(), log.getMessage(), log.getCreatedAt());
    }
}
