package io.github.onedream921.alphavue.framework.mybatis;

import java.util.regex.Pattern;

/**
 * 清洗并限制 SQL 执行摘要，确保采集链路不保存注释和字面量。
 */
public final class SqlLogSanitizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SQL_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/|--[^\\r\\n]*|#[^\\r\\n]*");
    private static final Pattern SQL_LITERAL = Pattern.compile("'(?:''|[^'])*'|\\\"(?:\\\"\\\"|[^\\\"])*\\\"");

    private SqlLogSanitizer() {
    }

    public static String normalize(String sql) {
        if (sql == null) {
            return "";
        }
        String withoutComments = SQL_COMMENT.matcher(sql).replaceAll(" ");
        String withoutLiterals = SQL_LITERAL.matcher(withoutComments).replaceAll("?");
        return WHITESPACE.matcher(withoutLiterals).replaceAll(" ").trim();
    }

    public static String bound(String sql, int maxLength) {
        String normalized = normalize(sql);
        int limit = Math.max(64, maxLength);
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit - 3) + "...";
    }
}
