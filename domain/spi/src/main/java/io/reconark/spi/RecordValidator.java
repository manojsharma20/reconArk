package io.reconark.spi;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.pipeline.Violation;
import java.util.List;
import java.util.Map;

/** Field-level or cross-field validation rule. Returns every violation, not just the first. */
public interface RecordValidator {

    List<Violation> validate(Map<String, Object> record, PluginConfig options);
}
