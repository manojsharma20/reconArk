package io.reconark.kernel.api;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validated, defaulted configuration handed to a plugin or a stage. Values come from the composition (platform) or
 * from a provider configuration version (business). Never contains secret values — only reference names.
 *
 * @param values validated values by property name
 */
public record PluginConfig(Map<String, Object> values) {

    /** Empty configuration. */
    public static final PluginConfig EMPTY = new PluginConfig(Map.of());

    public PluginConfig {
        values = Map.copyOf(values == null ? Map.of() : values);
    }

    public Optional<Object> get(String name) {
        return Optional.ofNullable(values.get(name));
    }

    public String string(String name) {
        return require(name).toString();
    }

    public Optional<String> optionalString(String name) {
        return get(name).map(Object::toString);
    }

    public int integer(String name) {
        Object v = require(name);
        return v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString());
    }

    public java.math.BigDecimal decimal(String name) {
        Object v = require(name);
        return v instanceof java.math.BigDecimal b ? b : new java.math.BigDecimal(v.toString());
    }

    public boolean bool(String name) {
        Object v = require(name);
        return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString());
    }

    public Duration duration(String name) {
        Object v = require(name);
        return v instanceof Duration d ? d : Duration.parse(v.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Object> list(String name) {
        Object v = require(name);
        if (v instanceof List<?> l) {
            return (List<Object>) l;
        }
        throw new KernelException(KernelError.INVALID_CONFIG, "Property '" + name + "' is not a list");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> map(String name) {
        Object v = require(name);
        if (v instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new KernelException(KernelError.INVALID_CONFIG, "Property '" + name + "' is not a map");
    }

    private Object require(String name) {
        Object v = values.get(name);
        if (v == null) {
            throw new KernelException(KernelError.INVALID_CONFIG, "Missing configuration property '" + name + "'");
        }
        return v;
    }
}
