package io.reconark.kernel.runtime;

import io.reconark.kernel.api.KernelError;
import io.reconark.kernel.api.KernelException;
import io.reconark.kernel.api.ReconArkPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/** Every plugin installed in this distribution (on the classpath or registered as a remote proxy). */
public final class PluginCatalog {

    private final Map<String, ReconArkPlugin> plugins;

    private PluginCatalog(Map<String, ReconArkPlugin> plugins) {
        this.plugins = Collections.unmodifiableMap(plugins);
    }

    /** Discovers plugins with {@link ServiceLoader} on the given class loader. */
    public static PluginCatalog discover(ClassLoader classLoader) {
        Map<String, ReconArkPlugin> found = new LinkedHashMap<>();
        for (ReconArkPlugin p : ServiceLoader.load(ReconArkPlugin.class, classLoader)) {
            add(found, p);
        }
        return new PluginCatalog(found);
    }

    /** A catalog of exactly the given plugins (tests, remote proxies). */
    public static PluginCatalog of(ReconArkPlugin... plugins) {
        Map<String, ReconArkPlugin> found = new LinkedHashMap<>();
        for (ReconArkPlugin p : plugins) {
            add(found, p);
        }
        return new PluginCatalog(found);
    }

    /** Returns a new catalog with {@code extra} added (e.g. remote plugin proxies). */
    public PluginCatalog with(ReconArkPlugin... extra) {
        Map<String, ReconArkPlugin> found = new LinkedHashMap<>(plugins);
        for (ReconArkPlugin p : extra) {
            add(found, p);
        }
        return new PluginCatalog(found);
    }

    private static void add(Map<String, ReconArkPlugin> found, ReconArkPlugin p) {
        String id = p.descriptor().id();
        if (found.putIfAbsent(id, p) != null) {
            throw new KernelException(KernelError.DUPLICATE_PLUGIN, "Plugin '" + id + "' is installed twice");
        }
    }

    public Optional<ReconArkPlugin> find(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    public Collection<ReconArkPlugin> all() {
        return plugins.values();
    }
}
