package com.github.fengzhizi319.privacy.sdk.dynclassification.operator;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Built-in matcher operators for classification rules.
 * 分类规则的内置匹配算子。
 */
public final class Operators {

    private Operators() {}

    /**
     * Registers all built-in operators to the given registry.
     * 将所有内置算子注册到给定的注册表。
     */
    public static void registerAll(OperatorRegistry registry) {
        registry.register("regex", Operators::regex);
        registry.register("keyword_contains", Operators::keywordContains);
        registry.register("keyword_equals", Operators::keywordEquals);
        registry.register("prefix", Operators::prefix);
        registry.register("suffix", Operators::suffix);
        registry.register("length_range", Operators::lengthRange);
        registry.register("id_card_checksum", Operators::idCardChecksum);
        registry.register("medical_card_checksum", Operators::medicalCardChecksum);
        registry.register("luhn_checksum", Operators::luhnChecksum);
        registry.register("icd10_range", Operators::icd10Range);
        registry.register("date_range", Operators::dateRange);
        registry.register("value_range", Operators::valueRange);
        registry.register("field_name_match", Operators::fieldNameMatch);
        registry.register("contains_digit_ratio", Operators::containsDigitRatio);
    }

    // --- Operator Implementations ---

    /**
     * Regex matcher with ReDoS protection (pattern length limit).
     * 正则匹配器，带 ReDoS 防护（模式长度限制）。
     */
    private static OperatorResult regex(String fieldName, String fieldValue, Map<String, Object> params) {
        String pattern = getString(params, "pattern", "");
        if (pattern.isEmpty() || fieldValue == null) {
            return OperatorResult.miss();
        }
        // ReDoS mitigation: limit pattern complexity
        // ReDoS 防护：限制模式复杂度
        if (pattern.length() > 512) {
            return OperatorResult.miss();
        }
        try {
            boolean caseInsensitive = getBool(params, "case_insensitive", false);
            int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
            if (Pattern.compile(pattern, flags).matcher(fieldValue).find()) {
                return OperatorResult.hit();
            }
        } catch (PatternSyntaxException e) {
            // Invalid pattern, treat as miss
        }
        return OperatorResult.miss();
    }

    /**
     * Keyword contains matcher.
     * 关键词包含匹配器。
     */
    private static OperatorResult keywordContains(String fieldName, String fieldValue, Map<String, Object> params) {
        List<String> keywords = getStringList(params, "keywords");
        List<String> fields = getStringList(params, "fields");
        
        // Determine which value to check
        String checkValue = fieldValue;
        if (!fields.isEmpty() && fields.contains("name")) {
            checkValue = fieldName;
        }
        if (checkValue == null) {
            return OperatorResult.miss();
        }
        
        String normalized = normalizeKeyword(checkValue);
        for (String kw : keywords) {
            if (normalized.contains(normalizeKeyword(kw))) {
                return OperatorResult.hit();
            }
        }
        return OperatorResult.miss();
    }

    /**
     * Keyword equals matcher.
     * 关键词相等匹配器。
     */
    private static OperatorResult keywordEquals(String fieldName, String fieldValue, Map<String, Object> params) {
        List<String> keywords = getStringList(params, "keywords");
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        String normalized = normalizeKeyword(fieldValue);
        for (String kw : keywords) {
            if (normalized.equals(normalizeKeyword(kw))) {
                return OperatorResult.hit();
            }
        }
        return OperatorResult.miss();
    }

    private static OperatorResult prefix(String fieldName, String fieldValue, Map<String, Object> params) {
        List<String> prefixes = getStringList(params, "prefixes");
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        for (String p : prefixes) {
            if (fieldValue.startsWith(p)) {
                return OperatorResult.hit();
            }
        }
        return OperatorResult.miss();
    }

    private static OperatorResult suffix(String fieldName, String fieldValue, Map<String, Object> params) {
        List<String> suffixes = getStringList(params, "suffixes");
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        for (String s : suffixes) {
            if (fieldValue.endsWith(s)) {
                return OperatorResult.hit();
            }
        }
        return OperatorResult.miss();
    }

    private static OperatorResult lengthRange(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        int min = getInt(params, "min", 0);
        int max = getInt(params, "max", Integer.MAX_VALUE);
        int len = fieldValue.length();
        if (len >= min && len <= max) {
            return OperatorResult.hit();
        }
        return OperatorResult.miss();
    }

    private static OperatorResult idCardChecksum(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null || fieldValue.length() != 18) {
            return OperatorResult.miss();
        }
        if (validateIDCard(fieldValue)) {
            return OperatorResult.hit();
        }
        return OperatorResult.miss();
    }

    private static OperatorResult medicalCardChecksum(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        // Medical card: 8-12 digits with optional check digit
        String digits = fieldValue.replaceAll("[^0-9]", "");
        if (digits.length() >= 8 && digits.length() <= 12 && validateLuhn(digits)) {
            return OperatorResult.hit();
        }
        return OperatorResult.miss();
    }

    private static OperatorResult luhnChecksum(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        String digits = fieldValue.replaceAll("[^0-9]", "");
        if (digits.length() >= 13 && validateLuhn(digits)) {
            return OperatorResult.hit();
        }
        return OperatorResult.miss();
    }

    private static OperatorResult icd10Range(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        String normalized = normalizeICD10(fieldValue);
        List<Map<String, Object>> ranges = getMapList(params, "ranges");
        for (Map<String, Object> range : ranges) {
            String from = normalizeICD10(getString(range, "from", ""));
            String to = normalizeICD10(getString(range, "to", ""));
            if (inICD10Interval(normalized, from, to)) {
                return OperatorResult.hit();
            }
        }
        return OperatorResult.miss();
    }

    private static OperatorResult dateRange(String fieldName, String fieldValue, Map<String, Object> params) {
        // Simple date range check - validates format and optional range
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        String pattern = getString(params, "pattern", "\\d{4}[-/]\\d{2}[-/]\\d{2}");
        try {
            if (Pattern.compile(pattern).matcher(fieldValue).find()) {
                return OperatorResult.hit();
            }
        } catch (PatternSyntaxException e) {
            // Invalid pattern
        }
        return OperatorResult.miss();
    }

    private static OperatorResult valueRange(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null) {
            return OperatorResult.miss();
        }
        try {
            double value = Double.parseDouble(fieldValue);
            double min = getDouble(params, "min", Double.MIN_VALUE);
            double max = getDouble(params, "max", Double.MAX_VALUE);
            if (value >= min && value <= max) {
                return OperatorResult.hit();
            }
        } catch (NumberFormatException e) {
            // Not a number
        }
        return OperatorResult.miss();
    }

    private static OperatorResult fieldNameMatch(String fieldName, String fieldValue, Map<String, Object> params) {
        List<String> patterns = getStringList(params, "patterns");
        if (fieldName == null) {
            return OperatorResult.miss();
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        for (String p : patterns) {
            try {
                if (Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(fieldName).find()) {
                    return OperatorResult.hit();
                }
            } catch (PatternSyntaxException e) {
                if (normalized.contains(p.toLowerCase(Locale.ROOT))) {
                    return OperatorResult.hit();
                }
            }
        }
        return OperatorResult.miss();
    }

    private static OperatorResult containsDigitRatio(String fieldName, String fieldValue, Map<String, Object> params) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return OperatorResult.miss();
        }
        double threshold = getDouble(params, "threshold", 0.5);
        long digitCount = fieldValue.chars().filter(Character::isDigit).count();
        double ratio = (double) digitCount / fieldValue.length();
        if (ratio >= threshold) {
            return OperatorResult.hit();
        }
        return OperatorResult.miss();
    }

    // --- Validation Helpers ---

    private static boolean validateIDCard(String id) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkChars = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = id.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            sum += (c - '0') * weights[i];
        }
        
        char expected = checkChars[sum % 11];
        char actual = Character.toUpperCase(id.charAt(17));
        return expected == actual;
    }

    private static boolean validateLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static String normalizeICD10(String code) {
        return code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static boolean inICD10Interval(String code, String from, String to) {
        if (from.isEmpty() && to.isEmpty()) {
            return false;
        }
        if (from.isEmpty()) {
            return code.compareTo(to) <= 0;
        }
        if (to.isEmpty()) {
            return code.compareTo(from) >= 0;
        }
        return code.compareTo(from) >= 0 && code.compareTo(to) <= 0;
    }

    // --- Parameter Helpers ---

    private static String normalizeKeyword(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]+", "");
    }

    private static String getString(Map<String, Object> params, String key, String defaultVal) {
        Object v = params.get(key);
        return v != null ? v.toString() : defaultVal;
    }

    private static boolean getBool(Map<String, Object> params, String key, boolean defaultVal) {
        Object v = params.get(key);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }
        return defaultVal;
    }

    private static int getInt(Map<String, Object> params, String key, int defaultVal) {
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt((String) v);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    private static double getDouble(Map<String, Object> params, String key, double defaultVal) {
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v instanceof List) {
            return (List<String>) v;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getMapList(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v instanceof List) {
            return (List<Map<String, Object>>) v;
        }
        return List.of();
    }
}
