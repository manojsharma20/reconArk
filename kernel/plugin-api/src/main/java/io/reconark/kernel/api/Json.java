package io.reconark.kernel.api;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Minimal JSON writing for schemas and inventories, so the kernel stays dependency-free. */
public final class Json {

    private Json() {}

    /** Quotes and escapes a string as a JSON string literal. */
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /** Renders a simple Java value (string, number, boolean, collection, map) as JSON. */
    public static String value(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        if (v instanceof Collection<?> c) {
            return c.stream().map(Json::value).collect(Collectors.joining(",", "[", "]"));
        }
        if (v instanceof Map<?, ?> m) {
            return m.entrySet().stream()
                    .map(e -> quote(String.valueOf(e.getKey())) + ":" + value(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        return quote(v.toString());
    }
}
