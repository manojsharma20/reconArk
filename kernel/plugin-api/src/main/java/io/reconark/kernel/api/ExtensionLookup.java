package io.reconark.kernel.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Read access to active extensions. Engines and services depend on this, never on plugin classes. */
public interface ExtensionLookup {

    /** The bound implementation of a {@link Cardinality#SINGLE} point. */
    <T> T single(ExtensionPoint<T> point);

    /** The implementation registered under {@code key} for a {@link Cardinality#KEYED} point. */
    <T> T keyed(ExtensionPoint<T> point, String key);

    /** Like {@link #keyed} but empty instead of failing when the key is not active. */
    <T> Optional<T> findKeyed(ExtensionPoint<T> point, String key);

    /** Active keys for a point (any cardinality). */
    Set<String> keys(ExtensionPoint<?> point);

    /** Ordered implementations of a {@link Cardinality#CHAIN} point. */
    <T> List<T> chain(ExtensionPoint<T> point);
}
