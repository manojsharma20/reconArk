package io.reconark.kernel.runtime;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Which bricks a service runs and how they snap together. Bound from {@code reconark.composition.*}.
 *
 * @param environment environment name; {@code prod} refuses {@code DEV_ONLY} plugins
 * @param plugins enabled plugins by id, with their configuration (an id listed under {@code enabled} gets defaults)
 * @param bindings for SINGLE points: extension point id to the active key
 * @param chains for CHAIN points: extension point id to the ordered keys
 * @param disabled plugin ids explicitly switched off (wins over {@code plugins})
 */
public record CompositionConfig(
        String environment,
        Map<String, PluginEntry> plugins,
        Map<String, String> bindings,
        Map<String, List<String>> chains,
        Set<String> disabled) {

    public CompositionConfig {
        environment = environment == null || environment.isBlank() ? "local" : environment;
        plugins = Map.copyOf(plugins == null ? Map.of() : plugins);
        bindings = Map.copyOf(bindings == null ? Map.of() : bindings);
        chains = Map.copyOf(chains == null ? Map.of() : chains);
        disabled = Set.copyOf(disabled == null ? Set.of() : disabled);
    }

    /** Whether production rules apply. */
    public boolean production() {
        return "prod".equalsIgnoreCase(environment) || "production".equalsIgnoreCase(environment);
    }

    /**
     * One enabled plugin.
     *
     * @param config raw configuration, validated against the plugin's {@code ConfigSpec}
     * @param remote set when the plugin runs out-of-process
     */
    public record PluginEntry(Map<String, Object> config, RemoteSpec remote) {
        public PluginEntry {
            config = config == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(config));
        }

        public static PluginEntry of(Map<String, Object> config) {
            return new PluginEntry(config, null);
        }
    }

    /**
     * Builds a composition from a generic map (as produced by YAML or Spring property binding):
     * <pre>{@code
     * environment: prod
     * enabled:  [ core-stages, format-delimited, bus-kafka ]          # plugins with default configuration
     * plugins:  { bus-kafka: { config: {...} }, remote-x: { remote: { endpoint: ..., timeout: PT2S } } }
     * bindings: { message-bus: kafka }
     * chains:   { audit-sink: [db-hash-chain, siem-stream] }
     * disabled: [ format-excel ]
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static CompositionConfig fromMap(Map<String, ?> map) {
        Objects.requireNonNull(map, "map");
        Map<String, PluginEntry> plugins = new LinkedHashMap<>();
        Object rawPlugins = map.get("plugins");
        if (rawPlugins instanceof Map<?, ?> pm) {
            pm.forEach((id, body) -> {
                Map<String, Object> b = body instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
                Map<String, Object> cfg = b.get("config") instanceof Map<?, ?> c ? (Map<String, Object>) c : Map.of();
                RemoteSpec remote = null;
                if (b.get("remote") instanceof Map<?, ?> r) {
                    Object t = r.get("timeout");
                    Object mb = r.get("max-batch");
                    remote = new RemoteSpec(
                            String.valueOf(r.get("endpoint")),
                            t == null ? null : Duration.parse(t.toString()),
                            mb == null ? 0 : Integer.parseInt(mb.toString()));
                }
                plugins.put(String.valueOf(id), new PluginEntry(cfg, remote));
            });
        }
        for (String id : toStringList(map.get("enabled"))) {
            plugins.putIfAbsent(id, PluginEntry.of(Map.of()));
        }
        Map<String, String> bindings = new LinkedHashMap<>();
        if (map.get("bindings") instanceof Map<?, ?> bm) {
            bm.forEach((k, v) -> bindings.put(String.valueOf(k), String.valueOf(v)));
        }
        Map<String, List<String>> chains = new LinkedHashMap<>();
        if (map.get("chains") instanceof Map<?, ?> cm) {
            cm.forEach((k, v) -> chains.put(String.valueOf(k), toStringList(v)));
        }
        Object env = map.get("environment");
        return new CompositionConfig(
                env == null ? null : env.toString(),
                plugins,
                bindings,
                chains,
                Set.copyOf(toStringList(map.get("disabled"))));
    }

    private static List<String> toStringList(Object v) {
        if (v instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        if (v instanceof Map<?, ?> m) { // Spring binds YAML lists of scalars as index-keyed maps in some cases
            return m.values().stream().map(String::valueOf).toList();
        }
        return v == null ? List.of() : List.of(v.toString().split("\\s*,\\s*"));
    }
}
