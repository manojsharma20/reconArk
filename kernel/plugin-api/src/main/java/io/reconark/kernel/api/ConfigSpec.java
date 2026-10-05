package io.reconark.kernel.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The configuration a plugin (or one of its extensions) accepts. Rendered as JSON Schema for the admin UI so that
 * every new plugin gets its configuration form for free.
 *
 * @param properties declared properties
 */
public record ConfigSpec(List<PropertySpec> properties) {

    /** A plugin that takes no configuration. */
    public static final ConfigSpec NONE = new ConfigSpec(List.of());

    public ConfigSpec {
        properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    public static ConfigSpec of(PropertySpec... properties) {
        return new ConfigSpec(List.of(properties));
    }

    /** Properties by name. */
    public Map<String, PropertySpec> byName() {
        return properties.stream().collect(Collectors.toUnmodifiableMap(PropertySpec::name, p -> p));
    }

    /** Renders this spec as a JSON Schema (draft 2020-12) document. Unknown properties are rejected. */
    public String toJsonSchema(String title) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"title\":")
                .append(Json.quote(title))
                .append(",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{");
        for (int i = 0; i < properties.size(); i++) {
            PropertySpec p = properties.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Json.quote(p.name())).append(":{\"type\":").append(Json.quote(jsonType(p.type())));
            if (p.type() == PropertyType.DURATION) {
                sb.append(",\"format\":\"duration\"");
            }
            if (!p.description().isEmpty()) {
                sb.append(",\"description\":").append(Json.quote(p.description()));
            }
            if (p.secretRef()) {
                sb.append(",\"x-reconark-secret-ref\":true");
            }
            if (p.defaultValue() != null) {
                sb.append(",\"default\":").append(Json.value(p.defaultValue()));
            }
            sb.append('}');
        }
        sb.append("},\"required\":[");
        sb.append(properties.stream()
                .filter(PropertySpec::required)
                .map(p -> Json.quote(p.name()))
                .collect(Collectors.joining(",")));
        return sb.append("]}").toString();
    }

    private static String jsonType(PropertyType t) {
        return switch (t) {
            case STRING, DURATION -> "string";
            case INTEGER -> "integer";
            case NUMBER -> "number";
            case BOOLEAN -> "boolean";
            case LIST -> "array";
            case MAP -> "object";
        };
    }
}
