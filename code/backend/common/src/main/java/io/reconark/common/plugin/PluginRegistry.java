package io.reconark.common.plugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Looks up plugins of one kind by id. Services build one registry per plugin kind from the plugins that are switched
 * on, e.g. {@code new PluginRegistry<>("format", formats)}.
 */
public final class PluginRegistry<T extends Plugin> {

    private final String kind;
    private final Map<String, T> byId = new TreeMap<>();

    public PluginRegistry(String kind, Collection<? extends T> plugins) {
        this.kind = kind;
        for (T plugin : plugins) {
            T previous = byId.put(plugin.id(), plugin);
            if (previous != null) {
                throw new IllegalStateException("Two " + kind + " plugins use the same id '" + plugin.id() + "': "
                        + previous.getClass().getName() + " and " + plugin.getClass().getName());
            }
        }
    }

    /** Returns the plugin with this id, or fails with a message that lists the ids that are available. */
    public T get(String id) {
        T plugin = byId.get(id);
        if (plugin == null) {
            throw new UnknownPluginException(kind, id, byId.keySet());
        }
        return plugin;
    }

    public boolean contains(String id) {
        return byId.containsKey(id);
    }

    /** All active plugins of this kind, sorted by id. */
    public List<T> all() {
        return List.copyOf(byId.values());
    }

    public String kind() {
        return kind;
    }
}
