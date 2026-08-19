package com.github.fengzhizi319.privacy.sdk.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 解析器（Minimal JSON Parser）。
 * <p>
 * 仅依赖 JDK 17 内置类库，支持解析对象、数组、字符串、数字、布尔值与 null。
 * 用于 {@code classifyJson} 等不方便引入外部 JSON 库的场景。
 * </p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class JsonParser {

    private final String json;
    private int pos;

    JsonParser(String json) {
        this.json = json == null ? "" : json;
        this.pos = 0;
    }

    /**
     * 解析 JSON 字符串为 Java 对象。
     *
     * @param json JSON 字符串
     * @return 对象或数组；解析失败时返回 {@code null}
     */
    public static Object parse(String json) {
        try {
            JsonParser parser = new JsonParser(json);
            parser.skipWhitespace();
            Object result = parser.parseValue();
            parser.skipWhitespace();
            return parser.pos >= parser.json.length() ? result : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length()) {
            return null;
        }
        char c = json.charAt(pos);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
                continue;
            }
            if (c == '}') {
                pos++;
                break;
            }
            throw new IllegalArgumentException("Expected , or } at " + pos);
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            Object value = parseValue();
            list.add(value);
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
                continue;
            }
            if (c == ']') {
                pos++;
                break;
            }
            throw new IllegalArgumentException("Expected , or ] at " + pos);
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\' && pos < json.length()) {
                char esc = json.charAt(pos++);
                switch (esc) {
                    case '"', '\\', '/' -> sb.append(esc);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 <= json.length()) {
                            String hex = json.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                    }
                    default -> sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unterminated string at " + pos);
    }

    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (json.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean at " + pos);
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new IllegalArgumentException("Invalid null at " + pos);
    }

    private Number parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
            pos++;
        }
        boolean isDouble = false;
        if (pos < json.length() && json.charAt(pos) == '.') {
            isDouble = true;
            pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            isDouble = true;
            pos++;
            if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        String num = json.substring(start, pos);
        if (isDouble) {
            return Double.parseDouble(num);
        }
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return Double.parseDouble(num);
        }
    }

    private void skipWhitespace() {
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private char peek() {
        return pos < json.length() ? json.charAt(pos) : '\0';
    }

    private void expect(char expected) {
        if (pos >= json.length() || json.charAt(pos) != expected) {
            throw new IllegalArgumentException("Expected " + expected + " at " + pos);
        }
        pos++;
    }
}
