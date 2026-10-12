package io.reconark.plugins.compare;

import io.reconark.common.plugin.FieldComparator;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Compares numbers, optionally within a tolerance. "10.0" equals "10.00"; with {@code tolerance=0.01}, "10.00" equals
 * "10.01".
 *
 * <p>Options: {@code tolerance} (default 0). Values that are not numbers are only equal when their text is identical.
 */
public class NumberComparator implements FieldComparator {

    @Override
    public String id() {
        return "number";
    }

    @Override
    public String description() {
        return "Numeric comparison. Option: tolerance (default 0), e.g. 0.01.";
    }

    @Override
    public boolean same(String left, String right, Map<String, String> options) {
        BigDecimal tolerance = new BigDecimal(options.getOrDefault("tolerance", "0"));
        BigDecimal l = parse(left);
        BigDecimal r = parse(right);
        if (l == null || r == null) {
            return ExactComparator.clean(left).equals(ExactComparator.clean(right));
        }
        return l.subtract(r).abs().compareTo(tolerance) <= 0;
    }

    private static BigDecimal parse(String value) {
        String clean = ExactComparator.clean(value);
        if (clean.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
