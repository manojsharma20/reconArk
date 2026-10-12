package io.reconark.plugins.compare;

import io.reconark.common.plugin.FieldComparator;
import java.util.Map;

/** Equal when both values are identical after trimming spaces. A missing value equals an empty value. */
public class ExactComparator implements FieldComparator {

    @Override
    public String id() {
        return "exact";
    }

    @Override
    public String description() {
        return "Values must be identical (leading/trailing spaces ignored).";
    }

    @Override
    public boolean same(String left, String right, Map<String, String> options) {
        return clean(left).equals(clean(right));
    }

    static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
