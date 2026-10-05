package io.reconark.plugins.recon;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.spi.Comparison;
import io.reconark.spi.FieldComparator;
import java.util.Map;

/** {@code value-map}: maps the right-side value through {@code mapping} before comparing (partner "00" == SUCCESS). */
final class ValueMapComparator implements FieldComparator {

    @Override
    public ConfigSpec parameters() {
        return ConfigSpec.of(
                PropertySpec.required("mapping", PropertyType.MAP, "right value -> canonical left value"),
                PropertySpec.optional("ignore-case", PropertyType.BOOLEAN, true, "compare mapped values ignoring case"));
    }

    @Override
    public Comparison compare(Object left, Object right, PluginConfig p) {
        Map<String, Object> mapping = p.map("mapping");
        String mapped = right == null ? null : String.valueOf(mapping.getOrDefault(right.toString(), right.toString()));
        String l = left == null ? null : left.toString();
        boolean equal = l == null || mapped == null
                ? l == mapped
                : (p.bool("ignore-case") ? l.equalsIgnoreCase(mapped) : l.equals(mapped));
        return equal ? Comparison.EQUAL : Comparison.different("'" + l + "' != '" + right + "' (mapped to '" + mapped + "')");
    }
}
