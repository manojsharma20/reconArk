package io.reconark.domain.engine.etl;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.pipeline.PipelineStage;
import io.reconark.kernel.pipeline.RecordEnvelope;
import io.reconark.kernel.pipeline.StageContext;
import io.reconark.kernel.pipeline.StagePayload;
import io.reconark.kernel.pipeline.Violation;
import java.util.List;

/**
 * {@code canonicalize}: checks the canonical core (record key, business date) and stamps provenance. The persistence
 * adapter turns accepted envelopes into {@code canonical_record} rows.
 */
final class CanonicalizeStage implements PipelineStage {

    static final String RECORD_KEY = "_record_key";
    static final String BUSINESS_DATE = "_business_date";

    @Override
    public ConfigSpec options() {
        return ConfigSpec.of(
                PropertySpec.required("record-key-field", PropertyType.STRING, "canonical field holding the business key"),
                PropertySpec.required("business-date-field", PropertyType.STRING, "canonical field holding the business date"));
    }

    @Override
    public StagePayload apply(StageContext context, StagePayload input) {
        if (!(input instanceof StagePayload.Records(List<RecordEnvelope> records))) {
            throw new IllegalStateException("canonicalize expects records");
        }
        String keyField = context.options().string("record-key-field");
        String dateField = context.options().string("business-date-field");
        for (RecordEnvelope r : records) {
            Object key = r.fields().get(keyField);
            Object date = r.fields().get(dateField);
            if (key == null || date == null) {
                r.reject(new Violation("RK-CAN-0001", key == null ? keyField : dateField, "canonical core field missing"));
                continue;
            }
            r.fields().put(RECORD_KEY, key.toString());
            r.fields().put(BUSINESS_DATE, date);
        }
        return input;
    }
}
