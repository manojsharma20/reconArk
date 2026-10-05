package io.reconark.kernel.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A typed socket into which plugins plug implementations.
 *
 * @param id stable kebab-case identifier, e.g. {@code format-reader}
 * @param type the Java interface implementations must implement
 * @param cardinality how many implementations may be active
 * @param version contract version; plugins declare the range they implement
 * @param <T> the contract type
 */
public record ExtensionPoint<T>(String id, Class<T> type, Cardinality cardinality, SemVer version) {

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    public ExtensionPoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(cardinality, "cardinality");
        Objects.requireNonNull(version, "version");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Extension point id must be kebab-case: " + id);
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension point type must be an interface: " + type.getName());
        }
    }

    /** Creates an extension point at contract version 1.0.0. */
    public static <T> ExtensionPoint<T> of(String id, Class<T> type, Cardinality cardinality) {
        return new ExtensionPoint<>(id, type, cardinality, SemVer.parse("1.0.0"));
    }
}
