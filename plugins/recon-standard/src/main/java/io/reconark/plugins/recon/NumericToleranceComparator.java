package io.reconark.plugins.recon;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.spi.Comparison;
import io.reconark.spi.FieldComparator;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * {@code numeric-tolerance}: rounds both sides ({@code scale}, half-up by default), then accepts an absolute and/or
 * percentage tolerance. Legacy behaviour "money rounded to 2 dp, half-up" is {@code scale: 2}.
 */
final class NumericToleranceComparator implements FieldComparator {

    @Override
    public ConfigSpec parameters() {
        return ConfigSpec.of(
                PropertySpec.optional("scale", PropertyType.INTEGER, 2, "decimal places after rounding"),
                PropertySpec.optional("rounding", PropertyType.STRING, "HALF_UP", "java.math.RoundingMode name"),
                PropertySpec.optional("absolute", PropertyType.NUMBER, "0", "absolute tolerance"),
                PropertySpec.optional("percent", PropertyType.NUMBER, "0", "percentage tolerance of the left value"));
    }

    @Override
    public Comparison compare(Object left, Object right, PluginConfig p) {
        if (left == null || right == null) {
            return left == right ? Comparison.EQUAL : Comparison.different("one side is missing");
        }
        BigDecimal l;
        BigDecimal r;
        try {
            int scale = p.integer("scale");
            RoundingMode mode = RoundingMode.valueOf(p.string("rounding"));
            l = new BigDecimal(left.toString().strip()).setScale(scale, mode);
            r = new BigDecimal(right.toString().strip()).setScale(scale, mode);
        } catch (NumberFormatException e) {
            return Comparison.different("not numeric");
        }
        BigDecimal diff = l.subtract(r).abs();
        BigDecimal allowed = p.decimal("absolute")
                .max(l.abs().multiply(p.decimal("percent")).divide(BigDecimal.valueOf(100), MathContext.DECIMAL64));
        return diff.compareTo(allowed) <= 0
                ? Comparison.EQUAL
                : Comparison.different(l.toPlainString() + " vs " + r.toPlainString() + " (difference " + diff.toPlainString()
                        + ", tolerance " + allowed.stripTrailingZeros().toPlainString() + ")");
    }
}
