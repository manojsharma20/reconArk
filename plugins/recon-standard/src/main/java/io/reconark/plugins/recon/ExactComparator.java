package io.reconark.plugins.recon;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.spi.Comparison;
import io.reconark.spi.FieldComparator;
import java.util.Objects;

/** {@code exact}: string forms are identical (null equals null). */
final class ExactComparator implements FieldComparator {
    @Override
    public Comparison compare(Object left, Object right, PluginConfig parameters) {
        String l = left == null ? null : left.toString();
        String r = right == null ? null : right.toString();
        return Objects.equals(l, r) ? Comparison.EQUAL : Comparison.different("'" + l + "' != '" + r + "'");
    }
}
