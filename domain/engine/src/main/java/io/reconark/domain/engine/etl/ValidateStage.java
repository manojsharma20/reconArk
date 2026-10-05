package io.reconark.domain.engine.etl;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.pipeline.PipelineStage;
import io.reconark.kernel.pipeline.RecordEnvelope;
import io.reconark.kernel.pipeline.StageContext;
import io.reconark.kernel.pipeline.StagePayload;
import io.reconark.kernel.pipeline.Violation;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.RecordValidator;
import java.util.List;
import java.util.Map;

/**
 * {@code validate}: runs the configured {@code record-validator} chain and collects every violation.
 *
 * <p>Options: {@code rules: [ { validator: <key>, options: {...} }, ... ]}.
 */
final class ValidateStage implements PipelineStage {

    @Override
    public ConfigSpec options() {
        return ConfigSpec.of(PropertySpec.optional("rules", PropertyType.LIST, List.of(), "validator key + options, in order"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StagePayload apply(StageContext context, StagePayload input) {
        if (!(input instanceof StagePayload.Records(List<RecordEnvelope> records))) {
            throw new IllegalStateException("validate expects records");
        }
        List<Object> rules = context.options().list("rules");
        for (RecordEnvelope r : records) {
            for (Object o : rules) {
                Map<String, Object> rule = (Map<String, Object>) o;
                RecordValidator v = context.extensions().keyed(ExtensionPoints.RECORD_VALIDATOR, String.valueOf(rule.get("validator")));
                Object opts = rule.get("options");
                List<Violation> violations = v.validate(r.fields(), new PluginConfig(opts instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of()));
                violations.forEach(r::reject);
            }
        }
        return input;
    }
}
