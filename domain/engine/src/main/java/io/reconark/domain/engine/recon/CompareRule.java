package io.reconark.domain.engine.recon;

import io.reconark.spi.FieldDiff;
import java.util.Map;
import java.util.Objects;

/**
 * One compared field pair.
 *
 * @param leftField canonical field on the left
 * @param rightField canonical field on the right
 * @param comparator {@code field-comparator} extension key
 * @param parameters comparator parameters, validated against its spec
 * @param severity blocking mismatch or warning
 * @param sensitive mask values in explanations
 */
public record CompareRule(
        String leftField,
        String rightField,
        String comparator,
        Map<String, Object> parameters,
        FieldDiff.Severity severity,
        boolean sensitive) {

    public CompareRule {
        Objects.requireNonNull(leftField, "leftField");
        Objects.requireNonNull(rightField, "rightField");
        Objects.requireNonNull(comparator, "comparator");
        parameters = parameters == null ? Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(parameters));
        severity = severity == null ? FieldDiff.Severity.BLOCKING : severity;
    }
}
