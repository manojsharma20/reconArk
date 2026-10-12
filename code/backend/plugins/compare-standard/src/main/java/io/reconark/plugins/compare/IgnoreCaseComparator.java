package io.reconark.plugins.compare;

import io.reconark.common.plugin.FieldComparator;
import java.util.Map;

/** Like {@code exact}, but upper and lower case are treated as equal ("Acme" = "ACME"). */
public class IgnoreCaseComparator implements FieldComparator {

    @Override
    public String id() {
        return "ignore-case";
    }

    @Override
    public String description() {
        return "Values must be identical ignoring upper/lower case.";
    }

    @Override
    public boolean same(String left, String right, Map<String, String> options) {
        return ExactComparator.clean(left).equalsIgnoreCase(ExactComparator.clean(right));
    }
}
