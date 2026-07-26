package io.github.onedream921.alphavue.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import org.springframework.stereotype.Service;

/**
 * 系统权限服务
 */
@Service
public class SystemAccessService {

    /**
     * 校验当前会话具备指定权限，超级管理员角色直接放行
     */
    public void require(String permission) {
        if (StpUtil.hasRole("SUPER_ADMIN") || StpUtil.hasPermission(permission)) {
            return;
        }
        throw new BusinessException(403, PublicErrorMessage.FORBIDDEN);
    }
}
