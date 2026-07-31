package io.github.onedream921.alphavue.modules.monitor.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.mapper.SysUserMapper;
import io.github.onedream921.alphavue.modules.system.vo.OnlineUserSummaryVo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OnlineSessionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int SESSION_SCAN_BATCH_SIZE = 100;
    private static final int MAX_SESSION_SCAN_SIZE = 10_000;
    private final SysUserMapper userMapper;

    public OnlineSessionService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResponse<OnlineSessionVo> page(int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        int start = (page - 1) * safeSize;
        List<OnlineSessionVo> all = collectPageCandidates(Integer.MAX_VALUE);
        List<OnlineSessionVo> records = all.stream().skip(start).limit(safeSize).toList();
        Map<Long, OnlineUserSummaryVo> users = loadUsers(records);
        List<OnlineSessionVo> enriched = records.stream()
                .map(record -> record.withUser(users.get(record.userId())))
                .toList();
        return new PageResponse<>(enriched, all.size(), page, safeSize);
    }

    private List<OnlineSessionVo> collectPageCandidates(int requiredSize) {
        List<OnlineSessionVo> records = new java.util.ArrayList<>();
        for (int offset = 0; offset < MAX_SESSION_SCAN_SIZE && records.size() < requiredSize; offset += SESSION_SCAN_BATCH_SIZE) {
            List<String> sessionIds = StpUtil.searchSessionId("", offset, SESSION_SCAN_BATCH_SIZE, true);
            if (sessionIds.isEmpty()) {
                break;
            }
            sessionIds.stream()
                    .map(StpUtil::getSessionBySessionId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(session -> session.getTerminalList().stream()
                            .map(terminal -> toView(session, terminal))
                            .forEach(records::add));
            if (sessionIds.size() < SESSION_SCAN_BATCH_SIZE) {
                break;
            }
        }
        return records;
    }

    public void kickout(long userId, int terminalIndex) {
        SaSession session = StpUtil.getSessionByLoginId(userId, false);
        if (session == null) {
            throw new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
        }
        SaTerminalInfo terminal = session.getTerminalList().stream()
                .filter(item -> item.getIndex() == terminalIndex)
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, PublicErrorMessage.INVALID_REQUEST));
        StpUtil.kickoutByTokenValue(terminal.getTokenValue());
    }

    private Map<Long, OnlineUserSummaryVo> loadUsers(Collection<OnlineSessionVo> records) {
        List<Long> ids = records.stream().map(OnlineSessionVo::userId).distinct().toList();
        Map<Long, OnlineUserSummaryVo> result = new HashMap<>();
        if (!ids.isEmpty()) {
            userMapper.selectActiveOnlineSummariesByIds(ids).forEach(user -> result.put(user.id(), user));
        }
        return result;
    }

    private OnlineSessionVo toView(SaSession session, SaTerminalInfo terminal) {
        String token = terminal.getTokenValue();
        long timeout = StpUtil.getTokenTimeout(token);
        Map<String, Object> extras = terminal.getExtraData() == null ? Map.of() : terminal.getExtraData();
        String userAgent = text(extras.get("userAgent"));
        return new OnlineSessionVo(
                Long.parseLong(session.getLoginId().toString()),
                terminal.getIndex(),
                shortToken(token),
                text(extras.get("clientId"), terminal.getDeviceType()),
                text(extras.get("deviceId"), terminal.getDeviceId()),
                text(extras.get("deviceName")),
                text(extras.get("ipAddress")),
                browser(userAgent),
                operatingSystem(userAgent),
                Instant.ofEpochMilli(terminal.getCreateTime()).toString(),
                Instant.ofEpochMilli(StpUtil.getStpLogic().getTokenLastActiveTime(token)).toString(),
                timeout,
                null);
    }

    private static String shortToken(String token) {
        String digest = io.github.onedream921.alphavue.framework.redis.RedisPhysicalKey.sha256(token);
        return digest.substring(0, 12);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String text(Object value, String fallback) {
        String result = text(value);
        return result == null || result.isBlank() ? fallback : result;
    }

    private static String browser(String userAgent) {
        if (userAgent == null) return "未知";
        if (userAgent.contains("Edg/")) return "Edge";
        if (userAgent.contains("Chrome/")) return "Chrome";
        if (userAgent.contains("Firefox/")) return "Firefox";
        if (userAgent.contains("Safari/")) return "Safari";
        return "未知";
    }

    private static String operatingSystem(String userAgent) {
        if (userAgent == null) return "未知";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac OS X")) return "macOS";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        if (userAgent.contains("Linux")) return "Linux";
        return "未知";
    }

    public record OnlineSessionVo(long userId, int terminalIndex, String tokenSummary, String clientId,
                                  String deviceId, String deviceName, String ipAddress, String browser,
                                  String operatingSystem, String loginTime, String lastActiveTime,
                                  long timeoutSeconds, OnlineUserSummaryVo user) {
        public OnlineSessionVo withUser(OnlineUserSummaryVo user) {
            return new OnlineSessionVo(userId, terminalIndex, tokenSummary, clientId, deviceId, deviceName,
                    ipAddress, browser, operatingSystem, loginTime, lastActiveTime, timeoutSeconds, user);
        }
    }
}
