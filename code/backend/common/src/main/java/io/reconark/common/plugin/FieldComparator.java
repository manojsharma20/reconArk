package io.reconark.common.plugin;

import java.util.Map;

/** Decides whether a field's left and right values are "the same", e.g. exactly, ignoring case, within a tolerance. */
public interface FieldComparator extends Plugin {

    /**
     * @param left    value from the left record, may be {@code null}
     * @param right   value from the right record, may be {@code null}
     * @param options options from the {@link io.reconark.common.model.CompareRule}, e.g. {@code tolerance=0.01}
     * @return {@code true} when the values should be treated as equal
     */
    boolean same(String left, String right, Map<String, String> options);
}
