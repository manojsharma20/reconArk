package io.reconark.spi;

import io.reconark.kernel.api.PluginConfig;
import java.util.Map;

/** Adds or derives fields (reference lookups, value maps, FX rates). Must be idempotent. */
public interface Enricher {

    void enrich(Map<String, Object> record, PluginConfig options);
}
