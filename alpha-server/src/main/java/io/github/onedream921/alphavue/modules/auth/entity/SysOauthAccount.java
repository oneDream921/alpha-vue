package io.github.onedream921.alphavue.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.onedream921.alphavue.modules.system.entity.SystemEntity;
import lombok.Getter;
import lombok.Setter;

/** Immutable third-party subject mapped to a local account. */
@Getter
@Setter
@TableName("sys_oauth_account")
public class SysOauthAccount extends SystemEntity {
    private String provider;
    private String subject;
    private Long userId;
    private String displayName;
    private String avatarUrl;
}
