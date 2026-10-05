package io.reconark.kernel.runtime;

import io.reconark.kernel.api.Cardinality;
import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.ExtensionPoint;
import io.reconark.kernel.api.KernelError;
import io.reconark.kernel.api.KernelException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The active extensions of one service, after composition. Thread-safe once sealed. */
public final class ExtensionRegistry implements ExtensionLookup {

    /** One contribution. */
    record Entry(ExtensionPoint<?> point, String key, String pluginId, Object implementation) {}

    private final Map<String, Map<String, Entry>> byPoint = new LinkedHashMap<>();
    private final Map<String, String> singleBindings = new LinkedHashMap<>();
    private final Map<String, List<String>> chainOrder = new LinkedHashMap<>();
    private volatile boolean sealed;

    <T> void add(ExtensionPoint<T> point, String key, String pluginId, T impl) {
        if (sealed) {
            throw new IllegalStateException("Registry is sealed");
        }
        Map<String, Entry> entries = byPoint.computeIfAbsent(point.id(), k -> new LinkedHashMap<>());
        Entry existing = entries.get(key);
        if (existing != null) {
            throw new KernelException(KernelError.DUPLICATE_CONTRIBUTION, "Extension '" + point.id() + ":" + key
                    + "' contributed by both '" + existing.pluginId() + "' and '" + pluginId + "'");
        }
        if (existing == null && !entries.isEmpty()) {
            ExtensionPoint<?> first = entries.values().iterator().next().point();
            if (!first.type().equals(point.type()) || first.cardinality() != point.cardinality()) {
                throw new KernelException(KernelError.TYPE_MISMATCH, "Extension point '" + point.id()
                        + "' declared with different types or cardinalities");
            }
        }
        entries.put(key, new Entry(point, key, pluginId, impl));
    }

    void bindSingle(String pointId, String key) {
        singleBindings.put(pointId, key);
    }

    void orderChain(String pointId, List<String> keys) {
        chainOrder.put(pointId, List.copyOf(keys));
    }

    void seal() {
        sealed = true;
    }

    Map<String, Map<String, Entry>> entries() {
        return Collections.unmodifiableMap(byPoint);
    }

    Map<String, String> singleBindings() {
        return Collections.unmodifiableMap(singleBindings);
    }

    @Override
    public <T> T single(ExtensionPoint<T> point) {
        requireCardinality(point, Cardinality.SINGLE);
        String key = singleBindings.get(point.id());
        if (key == null) {
            Map<String, Entry> entries = byPoint.getOrDefault(point.id(), Map.of());
            if (entries.size() == 1) {
                key = entries.keySet().iterator().next();
            } else {
                throw new KernelException(KernelError.MISSING_BINDING, "No binding for SINGLE point '" + point.id() + "'");
            }
        }
        return keyedInternal(point, key);
    }

    @Override
    public <T> T keyed(ExtensionPoint<T> point, String key) {
        return keyedInternal(point, key);
    }

    @Override
    public <T> Optional<T> findKeyed(ExtensionPoint<T> point, String key) {
        Entry e = byPoint.getOrDefault(point.id(), Map.of()).get(key);
        return e == null ? Optional.empty() : Optional.of(point.type().cast(e.implementation()));
    }

    @Override
    public Set<String> keys(ExtensionPoint<?> point) {
        return Collections.unmodifiableSet(byPoint.getOrDefault(point.id(), Map.of()).keySet());
    }

    @Override
    public <T> List<T> chain(ExtensionPoint<T> point) {
        requireCardinality(point, Cardinality.CHAIN);
        Map<String, Entry> entries = byPoint.getOrDefault(point.id(), Map.of());
        List<String> order = chainOrder.getOrDefault(point.id(), new ArrayList<>(entries.keySet()));
        List<T> out = new ArrayList<>(order.size());
        for (String key : order) {
            out.add(keyedInternal(point, key));
        }
        return Collections.unmodifiableList(out);
    }

    private <T> T keyedInternal(ExtensionPoint<T> point, String key) {
        Entry e = byPoint.getOrDefault(point.id(), Map.of()).get(key);
        if (e == null) {
            throw new KernelException(KernelError.NO_SUCH_EXTENSION, "No active extension '" + point.id() + ":" + key
                    + "'. Active keys: " + keys(point));
        }
        return point.type().cast(e.implementation());
    }

    private static void requireCardinality(ExtensionPoint<?> point, Cardinality expected) {
        if (point.cardinality() != expected) {
            throw new IllegalArgumentException("Extension point '" + point.id() + "' is " + point.cardinality()
                    + ", not " + expected);
        }
    }
}
