package io.reconark.plugins.recon;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.spi.Comparison;
import io.reconark.spi.FieldComparator;

/** {@code case-insensitive}: equal ignoring case and surrounding whitespace. */
final class CaseInsensitiveComparator implements FieldComparator {
    @Override
    public Comparison compare(Object left, Object right, PluginConfig parameters) {
        if (left == null || right == null) {
            return left == right ? Comparison.EQUAL : Comparison.different("one side is missing");
        }
        return left.toString().strip().equalsIgnoreCase(right.toString().strip())
                ? Comparison.EQUAL
                : Comparison.different("'" + left + "' != '" + right + "' (case-insensitive)");
    }
}
