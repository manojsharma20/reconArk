package io.reconark.platform.starter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code reconark.composition.*} — which plugins this service runs, their configuration and bindings.
 *
 * <pre>{@code
 * reconark:
 *   composition:
 *     environment: prod
 *     enabled: [core-stages, format-delimited, bus-kafka, secrets-aws]
 *     plugins:
 *       bus-kafka: { config: { bootstrap-servers: "${KAFKA_BOOTSTRAP}" } }
 *     bindings: { message-bus: kafka }
 *     disabled: [format-excel]
 * }</pre>
 */
@ConfigurationProperties(prefix = "reconark.composition")
public class ReconArkCompositionProperties {

    private String environment = "local";
    private List<String> enabled = new ArrayList<>();
    private Map<String, Map<String, Object>> plugins = new LinkedHashMap<>();
    private Map<String, String> bindings = new LinkedHashMap<>();
    private Map<String, List<String>> chains = new LinkedHashMap<>();
    private List<String> disabled = new ArrayList<>();

    /** Generic map understood by {@code CompositionConfig.fromMap}. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("environment", environment);
        m.put("enabled", enabled);
        m.put("plugins", plugins);
        m.put("bindings", bindings);
        m.put("chains", chains);
        m.put("disabled", disabled);
        return m;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getEnabled() {
        return enabled;
    }

    public void setEnabled(List<String> enabled) {
        this.enabled = enabled;
    }

    public Map<String, Map<String, Object>> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, Map<String, Object>> plugins) {
        this.plugins = plugins;
    }

    public Map<String, String> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, String> bindings) {
        this.bindings = bindings;
    }

    public Map<String, List<String>> getChains() {
        return chains;
    }

    public void setChains(Map<String, List<String>> chains) {
        this.chains = chains;
    }

    public List<String> getDisabled() {
        return disabled;
    }

    public void setDisabled(List<String> disabled) {
        this.disabled = disabled;
    }
}
