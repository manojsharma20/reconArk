package io.reconark.domain.engine.etl;

import io.reconark.domain.model.MaskingPolicy;
import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.pipeline.PipelineStage;
import io.reconark.kernel.pipeline.RecordEnvelope;
import io.reconark.kernel.pipeline.StageContext;
import io.reconark.kernel.pipeline.StagePayload;
import io.reconark.kernel.pipeline.Violation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code map}: source fields to canonical fields with type conversion, defaults and required checks.
 *
 * <p>Options: {@code fields: { <canonical>: { source: <name>, type: string|decimal|date|integer, pattern: ...,
 * required: true, default: ..., sensitive: false } }}. Nested-path extraction (JSONPath/XPath, build prompt §6.4)
 * is the job of the {@code field-extractor} extensions and runs before this stage.
 */
final class MapStage implements PipelineStage {

    @Override
    public ConfigSpec options() {
        return ConfigSpec.of(PropertySpec.required("fields", PropertyType.MAP, "canonical field -> mapping"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StagePayload apply(StageContext context, StagePayload input) {
        if (!(input instanceof StagePayload.Records(List<RecordEnvelope> records))) {
            throw new IllegalStateException("map expects records");
        }
        Map<String, Object> fields = context.options().map("fields");
        for (RecordEnvelope r : records) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (var e : fields.entrySet()) {
                Map<String, Object> m = (Map<String, Object>) e.getValue();
                String target = e.getKey();
                Object raw = r.fields().get(String.valueOf(m.getOrDefault("source", target)));
                if (raw == null || raw.toString().isBlank()) {
                    raw = m.get("default");
                }
                boolean sensitive = Boolean.parseBoolean(String.valueOf(m.getOrDefault("sensitive", "false")));
                if (raw == null) {
                    if (Boolean.parseBoolean(String.valueOf(m.getOrDefault("required", "false")))) {
                        r.reject(new Violation("RK-MAP-0001", target, "required field '" + target + "' is missing"));
                    }
                    continue;
                }
                try {
                    out.put(target, convert(raw.toString().strip(), String.valueOf(m.getOrDefault("type", "string")),
                            (String) m.get("pattern")));
                } catch (RuntimeException ex) {
                    String shown = sensitive ? MaskingPolicy.mask(raw) : raw.toString();
                    r.reject(new Violation("RK-MAP-0002", target, "field '" + target + "' value '" + shown
                            + "' is not a valid " + m.getOrDefault("type", "string")));
                }
            }
            if (r.pending()) {
                r.fields().clear();
                r.fields().putAll(out);
            }
        }
        return input;
    }

    private static Object convert(String v, String type, String pattern) {
        return switch (type) {
            case "string" -> v;
            case "decimal" -> new BigDecimal(v);
            case "integer" -> Long.parseLong(v);
            case "date" -> pattern == null ? LocalDate.parse(v) : LocalDate.parse(v, DateTimeFormatter.ofPattern(pattern));
            default -> throw new IllegalArgumentException("unknown type " + type);
        };
    }
}
