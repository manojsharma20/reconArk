package io.reconark.kernel.runtime;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.KernelError;
import io.reconark.kernel.api.KernelException;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validates raw configuration against a {@link ConfigSpec}, applies defaults and coerces types. */
public final class ConfigValidator {

    private ConfigValidator() {}

    /**
     * Validates {@code raw} for {@code owner}.
     *
     * @throws KernelException {@link KernelError#INVALID_CONFIG} listing every problem, not just the first
     */
    public static PluginConfig validate(String owner, ConfigSpec spec, Map<String, Object> raw) {
        List<String> problems = new ArrayList<>();
        Map<String, PropertySpec> byName = spec.byName();
        for (String key : raw.keySet()) {
            if (!byName.containsKey(key)) {
                problems.add("unknown property '" + key + "'");
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (PropertySpec p : spec.properties()) {
            Object v = raw.containsKey(p.name()) ? raw.get(p.name()) : p.defaultValue();
            if (v == null) {
                if (p.required()) {
                    problems.add("missing required property '" + p.name() + "'");
                }
                continue;
            }
            try {
                out.put(p.name(), coerce(p, v));
            } catch (IllegalArgumentException | DateTimeParseException e) {
                problems.add("property '" + p.name() + "' is not a valid " + p.type());
            }
        }
        if (!problems.isEmpty()) {
            throw new KernelException(KernelError.INVALID_CONFIG, "Invalid configuration for '" + owner + "': "
                    + String.join("; ", problems));
        }
        return new PluginConfig(out);
    }

    private static Object coerce(PropertySpec p, Object v) {
        return switch (p.type()) {
            case STRING -> v.toString();
            case INTEGER -> v instanceof Number n ? (Object) n.longValue() : (Object) Long.parseLong(v.toString().strip());
            case NUMBER -> v instanceof BigDecimal b ? b : new BigDecimal(v.toString().strip());
            case BOOLEAN -> {
                String s = v.toString().strip();
                if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("not a boolean");
                }
                yield Boolean.parseBoolean(s);
            }
            case DURATION -> v instanceof Duration d ? d : Duration.parse(v.toString().strip());
            case LIST -> {
                if (v instanceof List<?> l) {
                    yield List.copyOf(l);
                }
                if (v instanceof Map<?, ?> m) {
                    yield List.copyOf(m.values());
                }
                throw new IllegalArgumentException("not a list");
            }
            case MAP -> {
                if (v instanceof Map<?, ?> m) {
                    yield Map.copyOf(m);
                }
                throw new IllegalArgumentException("not a map");
            }
        };
    }
}
