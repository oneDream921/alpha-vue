package io.github.onedream921.alphavue.modules.monitor.service;

import cn.dev33.satoken.session.SaSession;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 Lettuce 的 Redis 键空间访问实现
 */
@Component
@Profile("!test")
class LettuceRedisKeyspace implements RedisKeyspace {
    private static final int OVERVIEW_SCAN_COUNT = 100;
    private static final int MAX_OVERVIEW_DISCOVERED_KEYS = 10_000;
    private static final int VALUE_MEMBER_LIMIT = 100;
    private static final int VALUE_CHAR_LIMIT = 16_384;
    private static final int HEX_BYTE_LIMIT = 8_192;
    private static final JdkSerializationRedisSerializer JDK_SERIALIZER = new JdkSerializationRedisSerializer();

    private final RedisConnectionFactory connectionFactory;

    LettuceRedisKeyspace(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public RedisScanResult scan(String prefix, String keyword, String cursor, int count) {
        return withCommands(commands -> {
            KeyScanCursor<byte[]> result = commands.scan(ScanCursor.of(cursor), new ScanArgs()
                    .match(pattern(prefix)).limit(count)).get();
            return new RedisScanResult(result.getKeys().stream()
                    .filter(key -> matchesKeyword(key, keyword))
                    .map(key -> metadata(commands, key))
                    .filter(Objects::nonNull).toList(),
                    result.getCursor());
        });
    }

    @Override
    public RedisKeyMetadata metadata(String key) {
        return withCommands(commands -> metadata(commands, key.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public boolean delete(String key) {
        return withCommands(commands -> commands.del(key.getBytes(StandardCharsets.UTF_8)).get() > 0);
    }

    @Override
    public RedisOverview overview() {
        return withCommands(commands -> {
            String serverInfo = commands.info("server").get();
            return new RedisOverview(
                    infoValue(serverInfo, "redis_version"),
                    longInfoValue(serverInfo, "uptime_in_seconds"),
                    longInfoValue(commands.info("memory").get(), "used_memory"),
                    longInfoValue(commands.info("clients").get(), "connected_clients"),
                    managedKeyCounts(commands));
        });
    }

    private RedisKeyMetadata metadata(RedisClusterAsyncCommands<byte[], byte[]> commands, byte[] rawKey) {
        try {
            String key = new String(rawKey, StandardCharsets.UTF_8);
            String type = commands.type(rawKey).get();
            if ("none".equals(type)) {
                return null;
            }
            RedisValuePreview value = value(commands, rawKey, type);
            return new RedisKeyMetadata(key, type, commands.ttl(rawKey).get(), commands.memoryUsage(rawKey).get(),
                    value.value(), value.truncated());
        } catch (Exception exception) {
            throw new IllegalStateException("Redis 元数据查询失败", exception);
        }
    }

    private RedisValuePreview value(RedisClusterAsyncCommands<byte[], byte[]> commands, byte[] rawKey, String type)
            throws Exception {
        return switch (type) {
            case "string" -> bytesPreview(commands.get(rawKey).get());
            case "hash" -> preview(commands.hgetall(rawKey).get().entrySet().stream()
                    .limit(VALUE_MEMBER_LIMIT)
                    .map(entry -> bytesPreview(entry.getKey()).value() + ": " + bytesPreview(entry.getValue()).value())
                    .toList()
                    .toString(), commands.hlen(rawKey).get() > VALUE_MEMBER_LIMIT);
            case "list" -> preview(commands.lrange(rawKey, 0, VALUE_MEMBER_LIMIT - 1).get().stream()
                    .map(value -> bytesPreview(value).value())
                    .toList()
                    .toString(), commands.llen(rawKey).get() > VALUE_MEMBER_LIMIT);
            case "set" -> preview(commands.smembers(rawKey).get().stream()
                    .limit(VALUE_MEMBER_LIMIT)
                    .map(value -> bytesPreview(value).value())
                    .toList()
                    .toString(), commands.scard(rawKey).get() > VALUE_MEMBER_LIMIT);
            case "zset" -> preview(commands.zrangeWithScores(rawKey, 0, VALUE_MEMBER_LIMIT - 1).get().stream()
                    .map(value -> bytesPreview(value.getValue()).value() + " (" + value.getScore() + ")")
                    .toList()
                    .toString(), commands.zcard(rawKey).get() > VALUE_MEMBER_LIMIT);
            default -> new RedisValuePreview("暂不支持展示 " + type + " 类型的值", false);
        };
    }

    private static RedisValuePreview preview(String value) {
        return preview(value, false);
    }

    private static RedisValuePreview preview(String value, boolean memberTruncated) {
        if (value == null) {
            return new RedisValuePreview(null, false);
        }
        boolean valueTruncated = value.length() > VALUE_CHAR_LIMIT;
        String preview = valueTruncated ? value.substring(0, VALUE_CHAR_LIMIT) : value;
        return new RedisValuePreview(preview, memberTruncated || valueTruncated);
    }

    private static RedisValuePreview bytesPreview(byte[] value) {
        if (value == null) {
            return new RedisValuePreview(null, false);
        }
        Object object = deserializeJdk(value);
        if (object != null) {
            return preview(objectToText(object));
        }
        String text = readableUtf8(value);
        if (text != null) {
            return preview(text);
        }
        return hexPreview(value);
    }

    private static Object deserializeJdk(byte[] value) {
        if (value.length < 4 || (value[0] & 0xff) != 0xac || (value[1] & 0xff) != 0xed) {
            return null;
        }
        try {
            return JDK_SERIALIZER.deserialize(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String objectToText(Object value) {
        if (value instanceof SaSession session) {
            return "SaSession{"
                    + "id=" + session.getId()
                    + ", type=" + session.getType()
                    + ", loginType=" + session.getLoginType()
                    + ", loginId=" + session.getLoginId()
                    + ", token=" + session.getToken()
                    + ", createTime=" + session.getCreateTime()
                    + ", terminalList=" + session.getTerminalList()
                    + ", dataMap=" + session.getDataMap()
                    + '}';
        }
        return String.valueOf(value);
    }

    private static String readableUtf8(byte[] value) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            String text = decoder.decode(ByteBuffer.wrap(value)).toString();
            return isReadableText(text) ? text : null;
        } catch (CharacterCodingException exception) {
            return null;
        }
    }

    private static boolean isReadableText(String text) {
        if (text.isEmpty()) {
            return true;
        }
        long controlCharacters = text.chars()
                .filter(character -> Character.isISOControl(character)
                        && character != '\n'
                        && character != '\r'
                        && character != '\t')
                .count();
        return controlCharacters * 20 <= text.length();
    }

    private static RedisValuePreview hexPreview(byte[] value) {
        int length = Math.min(value.length, HEX_BYTE_LIMIT);
        StringBuilder builder = new StringBuilder("HEX ");
        for (int index = 0; index < length; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02x", value[index] & 0xff));
        }
        if (value.length > HEX_BYTE_LIMIT) {
            builder.append(" ...");
        }
        return preview(builder.toString(), value.length > HEX_BYTE_LIMIT);
    }

    private Map<String, Long> managedKeyCounts(RedisClusterAsyncCommands<byte[], byte[]> commands) {
        Map<String, Long> counts = new LinkedHashMap<>();
        long discovered = 0;
        ScanCursor cursor = ScanCursor.INITIAL;
        do {
            try {
                KeyScanCursor<byte[]> result = commands.scan(cursor, new ScanArgs().match("*")
                        .limit(OVERVIEW_SCAN_COUNT)).get();
                discovered += result.getKeys().size();
                cursor = result;
            } catch (Exception exception) {
                throw new IllegalStateException("Redis 概览查询失败", exception);
            }
        } while (!cursor.isFinished() && discovered < MAX_OVERVIEW_DISCOVERED_KEYS);
        counts.put("全部 Redis 键", discovered);
        return counts;
    }

    private static String pattern(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "*";
        }
        return prefix + "*";
    }

    private static boolean matchesKeyword(byte[] rawKey, String keyword) {
        if (keyword == null) {
            return true;
        }
        String key = new String(rawKey, StandardCharsets.UTF_8);
        return key.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private <T> T withCommands(CommandCallback<T> callback) {
        try (LettuceConnection connection = (LettuceConnection) connectionFactory.getConnection()) {
            return callback.apply(connection.getNativeConnection());
        } catch (Exception exception) {
            throw new IllegalStateException("Redis 运维访问失败", exception);
        }
    }

    private static String infoValue(String info, String name) {
        for (String line : info.split("\\r?\\n")) {
            if (line.startsWith(name + ':')) {
                return line.substring(name.length() + 1);
            }
        }
        return null;
    }

    private static Long longInfoValue(String info, String name) {
        String value = infoValue(info, name);
        return value == null ? null : Long.parseLong(value);
    }

    @FunctionalInterface
    private interface CommandCallback<T> {
        T apply(RedisClusterAsyncCommands<byte[], byte[]> commands) throws Exception;
    }

    private record RedisValuePreview(String value, boolean truncated) {
    }
}
