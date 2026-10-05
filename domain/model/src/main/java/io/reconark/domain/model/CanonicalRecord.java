package io.reconark.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One transaction from any source, normalised to reconArk's canonical fields. Core columns are explicit; everything
 * else is a typed extension attribute in {@code fields} (no per-provider columns, build prompt §2.3 item 3).
 *
 * @param sourceId onboarded source code, e.g. {@code ACQUIRER_A}
 * @param recordKey business key unique within source and business date
 * @param businessDate business date in the bank's timezone
 * @param fields canonical field values by canonical field name
 */
public record CanonicalRecord(String sourceId, String recordKey, LocalDate businessDate, Map<String, Object> fields) {

    public CanonicalRecord {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(recordKey, "recordKey");
        Objects.requireNonNull(businessDate, "businessDate");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields == null ? Map.of() : fields));
    }

    /** A canonical field value, if present. */
    public Optional<Object> field(String name) {
        return Optional.ofNullable(fields.get(name));
    }
}
