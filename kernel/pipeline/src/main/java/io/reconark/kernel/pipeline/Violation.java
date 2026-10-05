package io.reconark.kernel.pipeline;

import java.util.Objects;

/**
 * Why a record (or one of its fields) failed a stage. {@code value} must already be masked if the field is sensitive.
 *
 * @param code stable reason code, e.g. {@code RK-VAL-0001}
 * @param field field name, or {@code null} for record-level problems
 * @param message human-readable message
 */
public record Violation(String code, String field, String message) {
    public Violation {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
