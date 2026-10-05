package io.reconark.kernel.api;

import java.util.Objects;

/**
 * One configuration property a plugin accepts.
 *
 * @param name kebab-case property name
 * @param type value type
 * @param required whether a value must be present (after defaults)
 * @param defaultValue default, or {@code null}
 * @param secretRef {@code true} if the value is a secret-manager reference name, never a secret value
 * @param description human-readable help, shown in the admin UI
 */
public record PropertySpec(
        String name, PropertyType type, boolean required, Object defaultValue, boolean secretRef, String description) {

    public PropertySpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        description = description == null ? "" : description;
    }

    public static PropertySpec required(String name, PropertyType type, String description) {
        return new PropertySpec(name, type, true, null, false, description);
    }

    public static PropertySpec optional(String name, PropertyType type, Object defaultValue, String description) {
        return new PropertySpec(name, type, false, defaultValue, false, description);
    }

    public static PropertySpec secretRef(String name, String description) {
        return new PropertySpec(name, PropertyType.STRING, true, null, true, description);
    }
}
