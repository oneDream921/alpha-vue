package io.github.onedream921.alphavue.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.parameter.enums.SaReplacedRange;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.auth.dto.LoginRequest;
import io.github.onedream921.alphavue.modules.auth.dto.LoginResponse;
import io.github.onedream921.alphavue.modules.auth.dto.ProfileRequests;
import io.github.onedream921.alphavue.modules.file.service.FileService;
import io.github.onedream921.alphavue.modules.log.service.AuditLogService;
import io.github.onedream921.alphavue.modules.system.entity.SysUser;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.auth.service.ClientRegistryService.Client;
import io.github.onedream921.alphavue.modules.system.vo.RouteVo;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.Duration;
import java.util.UUID;

/**
 * 认证服务
 */
@Service
public class AuthService {

    private static final long SESSION_TIMEOUT_SECONDS = 8 * 60 * 60;
    private static final long REMEMBERED_SESSION_TIMEOUT_SECONDS = 7 * 24 * 60 * 60;

    private final SysUserMapper userMapper;
    private final LoginFailureStore loginFailureStore;
    private final AuditLogService auditLogService;
    private final CaptchaService captchaService;
    private final FileService fileService;
    private final ClientRegistryService clientRegistryService;
    private final LoginSessionCoordinator loginSessionCoordinator;
    private final SystemSettingService settingService;

    public AuthService(SysUserMapper userMapper, LoginFailureStore loginFailureStore, AuditLogService auditLogService,
                       CaptchaService captchaService, FileService fileService,
                       ClientRegistryService clientRegistryService,
                       LoginSessionCoordinator loginSessionCoordinator, SystemSettingService settingService) {
        this.userMapper = userMapper;
        this.loginFailureStore = loginFailureStore;
        this.auditLogService = auditLogService;
        this.captchaService = captchaService;
        this.fileService = fileService;
        this.clientRegistryService = clientRegistryService;
        this.loginSessionCoordinator = loginSessionCoordinator;
        this.settingService = settingService;
    }

    /**
     * 校验验证码和密码，登录成功后签发会话 Token
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent, String traceId) {
        Client client = clientRegistryService.requireEnabled(request.clientId());
        captchaService.validate(request.captchaId(), request.captcha());
        if (!loginFailureStore.reserveAttempt(request.username(), ipAddress, maxRetry(), Duration.ofMinutes(lockMinutes()))) {
            auditLogService.recordLogin(request.username(), null, false, ipAddress, userAgent, request.clientId(),
                    request.deviceId(), request.deviceName(), traceId, 429, "Login temporarily locked");
            throw new BusinessException(429, PublicErrorMessage.LOGIN_TEMPORARILY_LOCKED);
        }

        UserAccount account = findAccount(request.username());
        if (account == null || !BCrypt.checkpw(request.password(), account.passwordHash())) {
            auditLogService.recordLogin(request.username(), null, false, ipAddress, userAgent, request.clientId(),
                    request.deviceId(), request.deviceName(), traceId, 401, "Invalid credentials");
            throw new BusinessException(401, PublicErrorMessage.INVALID_CREDENTIALS);
        }

        loginFailureStore.clear(request.username(), ipAddress);
        long timeout = rememberMeEnabled() && Boolean.TRUE.equals(request.rememberMe())
                ? REMEMBERED_SESSION_TIMEOUT_SECONDS : SESSION_TIMEOUT_SECONDS;
        return issueLogin(account.id(), account.username(), client, request.deviceId(), request.deviceName(), timeout,
                ipAddress, userAgent, traceId);
    }

    /** Issues a normal PC-admin session after a third-party identity has been mapped to an enabled account. */
    public LoginResponse loginFromExternalIdentity(long userId, String ipAddress, String userAgent, String traceId) {
        SysUser user = userMapper.selectActiveById(userId);
        if (user == null) throw new BusinessException(403, PublicErrorMessage.FORBIDDEN);
        Client client = clientRegistryService.requireEnabled("pc-admin");
        return issueLogin(user.getId(), user.getUsername(), client, null, "third-party", SESSION_TIMEOUT_SECONDS,
                ipAddress, userAgent, traceId);
    }

    private LoginResponse issueLogin(long userId, String username, Client client, String deviceId, String deviceName,
                                     long timeout, String ipAddress, String userAgent, String traceId) {
        SaLoginParameter loginParameter = SaLoginParameter.create().setTimeout(timeout)
                .setDevice(client.clientId())
                .setDeviceId(deviceId)
                .setIsConcurrent(false)
                .setReplacedRange(SaReplacedRange.CURR_DEVICE_TYPE)
                .setTerminalExtra("clientId", client.clientId())
                .setTerminalExtra("deviceName", deviceName)
                .setTerminalExtra("ipAddress", ipAddress)
                .setTerminalExtra("userAgent", userAgent);
        loginSessionCoordinator.execute(userId, client.clientId(), () -> {
            StpUtil.login(userId, loginParameter);
            return null;
        });
        auditLogService.recordLogin(username, userId, true, ipAddress, userAgent, client.clientId(),
                deviceId, deviceName, traceId, null, null);
        return new LoginResponse(StpUtil.getTokenValue(), "Bearer", timeout);
    }

    /**
     * 查询当前登录用户资料、角色和权限
     */
    public Profile profile() {
        long userId = currentUserId();
        SysUser user = userMapper.selectActiveById(userId);
        return user == null ? null : new Profile(user.getId(), user.getUsername(), user.getNickname(),
                resolveAvatar(user.getAvatar()), user.getEmail(), user.getPhone(), user.getDeptId(),
                Integer.valueOf(1).equals(user.getMustChangePassword()),
                StpUtil.getRoleList(), StpUtil.getPermissionList());
    }

    /**
     * 创建登录验证码挑战
     */
    public CaptchaService.CaptchaResponse captcha() {
        return captchaService.create();
    }

    /**
     * 查询当前登录用户可见路由
     */
    public List<RouteVo> routes() {
        return userMapper.selectVisibleRoutesByUserId(currentUserId());
    }

    /**
     * 更新当前登录用户资料
     */
    public Profile updateProfile(ProfileRequests.Update request) {
        long userId = currentUserId();
        userMapper.updateActiveProfile(userId, request.nickname(), request.avatar(), request.email(), request.phone());
        return profile();
    }

    /**
     * 上传并更新当前用户头像
     */
    public Profile uploadAvatar(MultipartFile avatarFile) {
        long userId = currentUserId();
        FileService.FileView uploaded = fileService.uploadAvatar(avatarFile, userId);
        if (userMapper.updateActiveAvatar(userId, "file:" + uploaded.id()) == 0) {
            throw new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
        }
        return profile();
    }

    /**
     * 修改当前登录用户密码并退出会话
     */
    public void changePassword(ProfileRequests.ChangePassword request) {
        long userId = currentUserId();
        SysUser user = userMapper.selectActiveById(userId);
        String passwordHash = user == null ? null : user.getPassword();
        if (passwordHash == null || !BCrypt.checkpw(request.currentPassword(), passwordHash)) {
            throw new BusinessException(400, PublicErrorMessage.CURRENT_PASSWORD_INCORRECT);
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException(400, PublicErrorMessage.PASSWORD_MUST_DIFFER);
        }
        userMapper.updateActivePassword(userId, BCrypt.hashpw(request.newPassword(), BCrypt.gensalt()));
        StpUtil.logout();
    }

    private UserAccount findAccount(String username) {
        SysUser user = userMapper.selectActiveByUsername(username);
        return user == null ? null : new UserAccount(user.getId(), user.getUsername(), user.getPassword());
    }

    private long currentUserId() {
        return Long.parseLong(StpUtil.getLoginIdAsString());
    }

    /**
     * 登录失败次数存储接口
     */
    public interface LoginFailureStore {
        /**
         * 在十五分钟窗口内原子占用五次允许尝试中的一次
         */
        boolean reserveAttempt(String username, String ipAddress, int limit, Duration window);

        /**
         * 清除指定账号和 IP 的登录失败记录
         */
        void clear(String username, String ipAddress);
    }

    private record UserAccount(long id, String username, String passwordHash) {
    }

    private int maxRetry() { return integer("maxRetry", 5, 1, 20); }
    private int lockMinutes() { return integer("lockMinutes", 15, 1, 1440); }
    private boolean rememberMeEnabled() {
        Object value = settingService.get(SettingGroup.LOGIN).values().get("rememberMeEnabled");
        return value instanceof Boolean bool ? bool : !(value instanceof String text) || Boolean.parseBoolean(text);
    }
    private int integer(String key, int fallback, int min, int max) {
        Object value = settingService.get(SettingGroup.LOGIN).values().get(key);
        try { int parsed = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value)); return Math.clamp(parsed, min, max); }
        catch (Exception ignored) { return fallback; }
    }

    /**
     * 当前登录用户资料
     */
    public record Profile(long id, String username, String nickname, String avatar, String email, String phone,
                          Long deptId, boolean mustChangePassword, List<String> roles, List<String> permissions) {
    }

    private String resolveAvatar(String avatar) {
        if (avatar != null && avatar.startsWith("file:")) {
            try {
                return fileService.accessUrl(Long.parseLong(avatar.substring("file:".length())));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return avatar;
    }
}
