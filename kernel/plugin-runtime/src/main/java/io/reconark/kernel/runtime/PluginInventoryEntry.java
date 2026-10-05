package io.reconark.kernel.runtime;

import io.reconark.kernel.api.HealthStatus;
import io.reconark.kernel.api.TrustTier;
import java.util.Map;
import java.util.Set;

/**
 * One row of the plugin inventory shown by {@code /actuator/plugins} and the operations API.
 *
 * @param id plugin id
 * @param version plugin version
 * @param name display name
 * @param trustTier trust tier
 * @param state lifecycle state
 * @param contributions extension point id to contributed keys
 * @param health current health
 * @param remote whether it runs out-of-process
 * @param configSchema JSON Schema of its configuration
 */
public record PluginInventoryEntry(
        String id,
        String version,
        String name,
        TrustTier trustTier,
        PluginState state,
        Map<String, Set<String>> contributions,
        HealthStatus health,
        boolean remote,
        String configSchema) {}
