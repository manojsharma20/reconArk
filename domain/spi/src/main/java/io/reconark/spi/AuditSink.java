package io.reconark.spi;

import java.time.Instant;
import java.util.Map;

/** Receives audit events; the chain typically writes the hash-chained table, then streams to the SIEM. */
public interface AuditSink {

    /**
     * @param at when it happened
     * @param actor user or service identity
     * @param action action code, e.g. {@code config.version.approved}
     * @param attributes masked attributes
     */
    void record(Instant at, String actor, String action, Map<String, String> attributes);
}
