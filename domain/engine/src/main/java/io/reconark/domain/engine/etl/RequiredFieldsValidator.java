package io.reconark.domain.engine.etl;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.pipeline.Violation;
import io.reconark.spi.RecordValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@code required}: options {@code fields: [a, b]}; every listed field must be present and non-blank. */
final class RequiredFieldsValidator implements RecordValidator {

    @Override
    public List<Violation> validate(Map<String, Object> record, PluginConfig options) {
        List<Violation> out = new ArrayList<>();
        for (Object f : options.list("fields")) {
            Object v = record.get(f.toString());
            if (v == null || v.toString().isBlank()) {
                out.add(new Violation("RK-VAL-0001", f.toString(), "field '" + f + "' is required"));
            }
        }
        return out;
    }
}
