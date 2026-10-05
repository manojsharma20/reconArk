package io.reconark.kernel.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One record travelling through a pipeline, with its fields, its position in the source and its disposition. */
public final class RecordEnvelope {

    /** Where a record ends up. */
    public enum Disposition { PENDING, ACCEPTED, REJECTED, QUARANTINED }

    private final long position;
    private final Map<String, Object> fields;
    private final List<Violation> violations = new ArrayList<>();
    private Disposition disposition = Disposition.PENDING;

    public RecordEnvelope(long position, Map<String, Object> fields) {
        this.position = position;
        this.fields = new LinkedHashMap<>(fields);
    }

    public long position() {
        return position;
    }

    /** Mutable field map; stages read and write it. */
    public Map<String, Object> fields() {
        return fields;
    }

    public List<Violation> violations() {
        return Collections.unmodifiableList(violations);
    }

    public Disposition disposition() {
        return disposition;
    }

    /** Rejects the record; all violations are collected, not just the first. */
    public RecordEnvelope reject(Violation violation) {
        violations.add(violation);
        disposition = Disposition.REJECTED;
        return this;
    }

    /** Whether this record is still flowing. */
    public boolean pending() {
        return disposition == Disposition.PENDING;
    }

    void settle(Disposition d) {
        if (disposition == Disposition.PENDING) {
            disposition = d;
        }
    }

    void quarantine() {
        disposition = Disposition.QUARANTINED;
    }
}
