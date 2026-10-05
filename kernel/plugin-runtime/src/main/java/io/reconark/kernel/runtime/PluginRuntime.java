package io.reconark.kernel.runtime;

import io.reconark.kernel.api.Cardinality;
import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.ExtensionPoint;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.KernelApi;
import io.reconark.kernel.api.KernelError;
import io.reconark.kernel.api.KernelException;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.api.TrustTier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Boots a {@link Kernel} from a {@link PluginCatalog} and a {@link CompositionConfig}. Every rule that can be checked
 * at startup is checked here, so a bad composition stops the service before it takes traffic ("fail fast at boot").
 *
 * <ol>
 *   <li>Every enabled plugin id exists in the catalog ({@code RK-KRN-0001}).
 *   <li>Each plugin supports this kernel API ({@code RK-KRN-0003}).
 *   <li>Trust tier is allowed in this environment ({@code RK-KRN-0004}).
 *   <li>Configuration is valid against the plugin's spec ({@code RK-KRN-0005}).
 *   <li>Requirements are provided by some enabled plugin; plugins register in dependency order ({@code RK-KRN-0006/0007}).
 *   <li>Plugins contribute only to points they declared ({@code RK-KRN-0014}); keys are unique ({@code RK-KRN-0010}).
 *   <li>Bindings and chains reference contributed keys ({@code RK-KRN-0008/0009}).
 * </ol>
 */
public final class PluginRuntime {

    private final List<ExtensionInterceptor> interceptors;

    public PluginRuntime() {
        this(List.of());
    }

    public PluginRuntime(List<ExtensionInterceptor> interceptors) {
        this.interceptors = List.copyOf(interceptors);
    }

    /** Boots the kernel or throws {@link KernelException}. */
    public Kernel boot(PluginCatalog catalog, CompositionConfig composition) {
        Map<String, PluginState> states = new LinkedHashMap<>();
        catalog.all().forEach(p -> states.put(p.descriptor().id(), PluginState.DISCOVERED));

        // 1-4: select and validate
        Map<String, ReconArkPlugin> enabled = new LinkedHashMap<>();
        Map<String, PluginConfig> configs = new HashMap<>();
        Set<String> remoteIds = new HashSet<>();
        for (var e : composition.plugins().entrySet()) {
            String id = e.getKey();
            if (composition.disabled().contains(id)) {
                states.put(id, PluginState.DISABLED);
                continue;
            }
            ReconArkPlugin plugin = catalog.find(id).orElseThrow(() -> new KernelException(
                    KernelError.UNKNOWN_PLUGIN, "Composition enables '" + id + "' but it is not installed"));
            PluginDescriptor d = plugin.descriptor();
            if (!d.kernelApi().contains(KernelApi.VERSION)) {
                throw new KernelException(KernelError.INCOMPATIBLE_KERNEL_API, "Plugin '" + id + "' supports kernel API "
                        + d.kernelApi() + " but this kernel is " + KernelApi.VERSION);
            }
            if (d.trustTier() == TrustTier.DEV_ONLY && composition.production()) {
                throw new KernelException(KernelError.TRUST_TIER_NOT_ALLOWED,
                        "Plugin '" + id + "' is DEV_ONLY and cannot run in environment '" + composition.environment() + "'");
            }
            if (d.trustTier() == TrustTier.REMOTE && e.getValue().remote() == null) {
                throw new KernelException(KernelError.TRUST_TIER_NOT_ALLOWED,
                        "Plugin '" + id + "' is REMOTE and must be configured with a remote endpoint");
            }
            if (e.getValue().remote() != null) {
                remoteIds.add(id);
            }
            configs.put(id, ConfigValidator.validate(id, d.config(), e.getValue().config()));
            enabled.put(id, plugin);
            states.put(id, PluginState.VALIDATED);
        }

        // 5: dependency order
        List<ReconArkPlugin> order = dependencyOrder(enabled);

        // 6: register
        ExtensionRegistry registry = new ExtensionRegistry();
        List<ReconArkPlugin> registered = new ArrayList<>();
        try {
            for (ReconArkPlugin plugin : order) {
                PluginDescriptor d = plugin.descriptor();
                ExtensionRegistrar registrar = new ScopedRegistrar(d, registry, interceptors);
                PluginContext ctx = new Context(configs.get(d.id()), composition.environment(), registry);
                try {
                    plugin.register(registrar, ctx);
                } catch (KernelException ke) {
                    throw ke;
                } catch (RuntimeException ex) {
                    states.put(d.id(), PluginState.FAILED);
                    throw new KernelException(KernelError.REGISTRATION_FAILED, "Plugin '" + d.id() + "' failed to register: "
                            + ex.getMessage(), ex);
                }
                registered.add(plugin);
                states.put(d.id(), PluginState.REGISTERED);
            }

            // 7: bindings and chains
            applyBindings(registry, composition);
            registry.seal();
        } catch (RuntimeException ex) {
            closeQuietly(registered);
            throw ex;
        }
        registered.forEach(p -> states.put(p.descriptor().id(), PluginState.ACTIVE));
        states.replaceAll((id, st) -> st == PluginState.DISCOVERED ? PluginState.DISABLED : st);
        return new Kernel(composition, registry, registered, states, remoteIds);
    }

    private static void applyBindings(ExtensionRegistry registry, CompositionConfig composition) {
        Map<String, Map<String, ExtensionRegistry.Entry>> entries = registry.entries();
        for (var b : composition.bindings().entrySet()) {
            Map<String, ExtensionRegistry.Entry> forPoint = entries.getOrDefault(b.getKey(), Map.of());
            if (!forPoint.containsKey(b.getValue())) {
                throw new KernelException(KernelError.UNKNOWN_BINDING_KEY, "Binding '" + b.getKey() + ": " + b.getValue()
                        + "' does not match any active extension. Active keys: " + forPoint.keySet());
            }
            registry.bindSingle(b.getKey(), b.getValue());
        }
        for (var c : composition.chains().entrySet()) {
            Map<String, ExtensionRegistry.Entry> forPoint = entries.getOrDefault(c.getKey(), Map.of());
            for (String key : c.getValue()) {
                if (!forPoint.containsKey(key)) {
                    throw new KernelException(KernelError.UNKNOWN_BINDING_KEY, "Chain '" + c.getKey() + "' references '"
                            + key + "' which is not active. Active keys: " + forPoint.keySet());
                }
            }
            registry.orderChain(c.getKey(), c.getValue());
        }
        // every SINGLE point with several contributions needs an explicit binding
        for (var e : entries.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }
            ExtensionPoint<?> point = e.getValue().values().iterator().next().point();
            if (point.cardinality() == Cardinality.SINGLE && e.getValue().size() > 1
                    && !composition.bindings().containsKey(e.getKey())) {
                throw new KernelException(KernelError.MISSING_BINDING, "SINGLE point '" + e.getKey() + "' has "
                        + e.getValue().keySet() + " active; add 'bindings." + e.getKey() + "' to the composition");
            }
        }
    }

    private static List<ReconArkPlugin> dependencyOrder(Map<String, ReconArkPlugin> enabled) {
        Map<String, Set<String>> providersByPoint = new HashMap<>();
        enabled.values().forEach(p -> p.descriptor().provides()
                .forEach(point -> providersByPoint.computeIfAbsent(point, k -> new HashSet<>()).add(p.descriptor().id())));
        Map<String, Set<String>> dependsOn = new LinkedHashMap<>();
        for (ReconArkPlugin p : enabled.values()) {
            Set<String> deps = new HashSet<>();
            for (String required : p.descriptor().requires()) {
                Set<String> providers = providersByPoint.get(required);
                if (providers == null || providers.isEmpty()) {
                    throw new KernelException(KernelError.UNMET_REQUIREMENT, "Plugin '" + p.descriptor().id()
                            + "' requires '" + required + "' but no enabled plugin provides it");
                }
                providers.stream().filter(id -> !id.equals(p.descriptor().id())).forEach(deps::add);
            }
            dependsOn.put(p.descriptor().id(), deps);
        }
        List<ReconArkPlugin> out = new ArrayList<>();
        Set<String> done = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String id : dependsOn.keySet()) {
            visit(id, dependsOn, enabled, done, visiting, new ArrayDeque<>(), out);
        }
        return out;
    }

    private static void visit(
            String id,
            Map<String, Set<String>> dependsOn,
            Map<String, ReconArkPlugin> enabled,
            Set<String> done,
            Set<String> visiting,
            Deque<String> path,
            List<ReconArkPlugin> out) {
        if (done.contains(id)) {
            return;
        }
        path.addLast(id);
        if (!visiting.add(id)) {
            throw new KernelException(KernelError.DEPENDENCY_CYCLE, "Plugin dependency cycle: " + String.join(" -> ", path));
        }
        for (String dep : dependsOn.get(id)) {
            visit(dep, dependsOn, enabled, done, visiting, path, out);
        }
        visiting.remove(id);
        path.removeLast();
        done.add(id);
        out.add(enabled.get(id));
    }

    private static void closeQuietly(List<ReconArkPlugin> plugins) {
        for (int i = plugins.size() - 1; i >= 0; i--) {
            try {
                plugins.get(i).close();
            } catch (Exception ignored) {
                // best effort during failed boot
            }
        }
    }

    private record Context(PluginConfig config, String environment, ExtensionLookup lookup) implements PluginContext {}

    /** Registrar scoped to one plugin: enforces declared {@code provides} and applies interceptors. */
    private record ScopedRegistrar(PluginDescriptor descriptor, ExtensionRegistry registry, List<ExtensionInterceptor> chain)
            implements ExtensionRegistrar {
        @Override
        public <T> void contribute(ExtensionPoint<T> point, String key, T implementation) {
            if (!descriptor.provides().contains(point.id())) {
                throw new KernelException(KernelError.UNDECLARED_CONTRIBUTION, "Plugin '" + descriptor.id()
                        + "' contributes to '" + point.id() + "' without declaring it in 'provides'");
            }
            if (!point.type().isInstance(implementation)) {
                throw new KernelException(KernelError.TYPE_MISMATCH, "Plugin '" + descriptor.id() + "' contributed a "
                        + implementation.getClass().getName() + " to '" + point.id() + "'");
            }
            T wrapped = Interception.wrap(point, key, descriptor.id(), implementation, chain);
            registry.add(point, key, descriptor.id(), wrapped);
        }
    }
}
