package io.reconark.kernel.runtime;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.HealthStatus;
import io.reconark.kernel.api.ReconArkPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A booted kernel: the active extensions plus the inventory. Close it to stop plugins in reverse order. */
public final class Kernel implements AutoCloseable {

    private final CompositionConfig composition;
    private final ExtensionRegistry registry;
    private final List<ReconArkPlugin> activeInOrder;
    private final Map<String, PluginState> states;
    private final Set<String> remoteIds;

    Kernel(
            CompositionConfig composition,
            ExtensionRegistry registry,
            List<ReconArkPlugin> activeInOrder,
            Map<String, PluginState> states,
            Set<String> remoteIds) {
        this.composition = composition;
        this.registry = registry;
        this.activeInOrder = List.copyOf(activeInOrder);
        this.states = new LinkedHashMap<>(states);
        this.remoteIds = Set.copyOf(remoteIds);
    }

    /** Lookup used by engines and services. */
    public ExtensionLookup extensions() {
        return registry;
    }

    public CompositionConfig composition() {
        return composition;
    }

    /** Inventory of every installed plugin, active or not. */
    public List<PluginInventoryEntry> inventory(PluginCatalog catalog) {
        List<PluginInventoryEntry> out = new ArrayList<>();
        for (ReconArkPlugin p : catalog.all()) {
            var d = p.descriptor();
            PluginState state = states.getOrDefault(d.id(), PluginState.DISABLED);
            Map<String, Set<String>> contributions = new LinkedHashMap<>();
            registry.entries().forEach((point, entries) -> entries.values().stream()
                    .filter(e -> e.pluginId().equals(d.id()))
                    .forEach(e -> contributions.computeIfAbsent(point, k -> new LinkedHashSet<>()).add(e.key())));
            HealthStatus health = state == PluginState.ACTIVE ? safeHealth(p) : new HealthStatus(HealthStatus.State.DOWN, state.name());
            out.add(new PluginInventoryEntry(
                    d.id(),
                    d.version().toString(),
                    d.name(),
                    d.trustTier(),
                    state,
                    Collections.unmodifiableMap(contributions),
                    health,
                    remoteIds.contains(d.id()),
                    d.config().toJsonSchema(d.name())));
        }
        return out;
    }

    /** Overall health: DOWN if any active plugin is DOWN. */
    public HealthStatus health() {
        for (ReconArkPlugin p : activeInOrder) {
            HealthStatus h = safeHealth(p);
            if (h.state() == HealthStatus.State.DOWN) {
                return HealthStatus.down(p.descriptor().id() + ": " + h.detail());
            }
        }
        return HealthStatus.UP;
    }

    private static HealthStatus safeHealth(ReconArkPlugin p) {
        try {
            return p.health();
        } catch (RuntimeException e) {
            return HealthStatus.down(e.getClass().getSimpleName());
        }
    }

    @Override
    public void close() {
        for (int i = activeInOrder.size() - 1; i >= 0; i--) {
            ReconArkPlugin p = activeInOrder.get(i);
            try {
                p.close();
            } catch (Exception ignored) {
                // stopping must continue for the remaining plugins
            }
            states.put(p.descriptor().id(), PluginState.STOPPED);
        }
    }
}
