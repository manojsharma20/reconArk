package io.reconark.spi;

/**
 * One explained difference.
 *
 * @param leftField canonical field on the left
 * @param rightField canonical field on the right
 * @param comparator comparator key
 * @param severity {@code BLOCKING} or {@code WARNING}
 * @param explanation comparator explanation (masked)
 */
public record FieldDiff(String leftField, String rightField, String comparator, Severity severity, String explanation) {

    /** Whether a difference blocks the match. */
    public enum Severity { BLOCKING, WARNING }
}
