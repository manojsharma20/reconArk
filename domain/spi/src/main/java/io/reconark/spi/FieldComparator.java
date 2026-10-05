package io.reconark.spi;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;

/**
 * Compares one field pair. Each comparator is one strategy in one registry; nothing compares fields outside it
 * (build prompt §6.6).
 */
public interface FieldComparator {

    /** Parameters this comparator accepts (tolerance, timezone, value map ...). */
    default ConfigSpec parameters() {
        return ConfigSpec.NONE;
    }

    /** Compares; {@code null} on either side is handled by the comparator. */
    Comparison compare(Object left, Object right, PluginConfig parameters);
}
