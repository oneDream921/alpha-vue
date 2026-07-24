package io.github.onedream921.alphavue.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import org.springframework.stereotype.Service;

/** Applies a deliberate all-system bypass only to the built-in super-administrator role. */
@Service
public class SystemAccessService {

    public void require(String permission) {
        if (StpUtil.hasRole("SUPER_ADMIN") || StpUtil.hasPermission(permission)) {
            return;
        }
        throw new BusinessException(403, PublicErrorMessage.FORBIDDEN);
    }
}
