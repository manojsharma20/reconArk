package io.reconark.kernel.pipeline;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.PluginConfig;
import java.util.Map;

/**
 * Everything a stage needs for one execution.
 *
 * @param runId the run this unit of work belongs to
 * @param stageKey key of the stage being executed
 * @param options validated stage options from the provider configuration version
 * @param extensions active extensions (format readers, validators, ...) for stages that delegate
 * @param attributes run-scoped attributes (source id, business date, config version); read-only
 */
public record StageContext(
        String runId, String stageKey, PluginConfig options, ExtensionLookup extensions, Map<String, Object> attributes) {
    public StageContext {
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
    }
}
