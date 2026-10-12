package io.reconark.common.model;

import java.util.Map;

/**
 * How to compare one field once a left and a right record have been paired.
 *
 * @param field      business field name, e.g. {@code amount}
 * @param comparator id of the {@link io.reconark.common.plugin.FieldComparator} plugin, e.g. {@code number}
 * @param options    options for the comparator, e.g. {@code tolerance=0.01}
 */
public record CompareRule(String field, String comparator, Map<String, String> options) {

    public CompareRule {
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
