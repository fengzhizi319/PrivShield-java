package com.github.fengzhizi319.privacy.sdk.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 脱敏工具类（Redact Utility）。
 * <p>
 * 实现 Zero-Knowledge 原则下的日志脱敏，确保敏感字段值最多保留前 N 个字符。
 * 对齐 privacy-local-agent 的 redact 设计与 review_export_mask 规范。
 * </p>
 * <p>
 * Redaction utility implementing Zero-Knowledge principle for log sanitization.
 * Ensures sensitive field values retain at most the first N characters.
 * </p>
 *
 * @author fengzhizi319
 * @since 0.2.0
 */
public final class RedactUtil {

    /** 默认保留字符数。Default max characters to retain. */
    private static final int DEFAULT_MAX_CHARS = 8;

    /** 脱敏后缀。Redaction suffix. */
    private static final String REDACT_SUFFIX = "***";

    /** 默认敏感键集合。Default sensitive key set. */
    private static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
        "id_card", "idcard", "identity",
        "mobile", "phone", "telephone",
        "email", "mail",
        "bank_card", "bankcard", "card_no",
        "password", "passwd", "secret",
        "token", "access_token", "refresh_token",
        "ssn", "social_security",
        "address", "addr",
        "name", "real_name", "fullname",
        "medical_record", "diagnosis"
    );

    private RedactUtil() {
        // 工具类禁止实例化 / Utility class, no instantiation
    }

    /**
     * 脱敏单个值，保留前 maxChars 个字符。
     * Redact a single value, retaining first maxChars characters.
     *
     * @param value    原始值 / original value
     * @param maxChars 最大保留字符数 / max characters to retain
     * @return 脱敏后的字符串 / redacted string
     */
    public static String redact(String value, int maxChars) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (maxChars <= 0) {
            return REDACT_SUFFIX;
        }
        if (value.length() <= maxChars) {
            return value + REDACT_SUFFIX;
        }
        return value.substring(0, maxChars) + REDACT_SUFFIX;
    }

    /**
     * 使用默认配置脱敏（保留前 8 字符）。
     * Redact with default settings (retain first 8 characters).
     *
     * @param value 原始值 / original value
     * @return 脱敏后的字符串 / redacted string
     */
    public static String redact(String value) {
        return redact(value, DEFAULT_MAX_CHARS);
    }

    /**
     * 对 Map 中的敏感键进行脱敏。
     * Redact sensitive keys in a Map.
     * <p>
     * 使用默认敏感键集合自动识别并脱敏。
     * Automatically identifies and redacts using default sensitive key set.
     * </p>
     *
     * @param data 原始数据 / original data
     * @return 脱敏后的新 Map（不修改原始数据）/ new redacted Map (original unchanged)
     */
    public static Map<String, Object> redactMap(Map<String, Object> data) {
        return redactMap(data, DEFAULT_SENSITIVE_KEYS, DEFAULT_MAX_CHARS);
    }

    /**
     * 对 Map 中的指定敏感键进行脱敏。
     * Redact specified sensitive keys in a Map.
     *
     * @param data          原始数据 / original data
     * @param sensitiveKeys 敏感键集合 / sensitive key set
     * @param maxChars      最大保留字符数 / max characters to retain
     * @return 脱敏后的新 Map / new redacted Map
     */
    public static Map<String, Object> redactMap(Map<String, Object> data, Set<String> sensitiveKeys, int maxChars) {
        if (data == null || data.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>(data.size());
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isSensitiveKey(key, sensitiveKeys) && value instanceof String) {
                result.put(key, redact((String) value, maxChars));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 判断键名是否为敏感键。
     * Determine if a key is sensitive.
     *
     * @param key           键名 / key name
     * @param sensitiveKeys 敏感键集合 / sensitive key set
     * @return 是否敏感 / whether sensitive
     */
    public static boolean isSensitiveKey(String key, Set<String> sensitiveKeys) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String normalizedKey = key.toLowerCase().replace("_", "").replace("-", "");
        for (String sensitive : sensitiveKeys) {
            String normalizedSensitive = sensitive.toLowerCase().replace("_", "").replace("-", "");
            if (normalizedKey.contains(normalizedSensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 用于日志输出的脱敏方法。
     * Redaction method for log output.
     * <p>
     * 与 {@link #redact(String)} 相同，但语义更明确表示用于日志场景。
     * Same as {@link #redact(String)}, but semantically indicates log usage.
     * </p>
     *
     * @param value 原始值 / original value
     * @return 脱敏后的字符串 / redacted string
     */
    public static String redactForLog(String value) {
        return redact(value, DEFAULT_MAX_CHARS);
    }

    /**
     * 完全遮蔽值（不保留任何字符）。
     * Fully mask a value (retain no characters).
     *
     * @return 固定遮蔽字符串 / fixed mask string
     */
    public static String fullMask() {
        return "[REDACTED]";
    }

    /**
     * 获取默认敏感键集合。
     * Get default sensitive key set.
     *
     * @return 敏感键集合 / sensitive key set
     */
    public static Set<String> getDefaultSensitiveKeys() {
        return DEFAULT_SENSITIVE_KEYS;
    }
}
