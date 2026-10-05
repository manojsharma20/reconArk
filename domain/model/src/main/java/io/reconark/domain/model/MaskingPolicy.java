package io.reconark.domain.model;

/** Masks sensitive values to their last four characters wherever they leave the database (build prompt §2.1). */
public final class MaskingPolicy {

    private MaskingPolicy() {}

    /** {@code AE070331234567891234} becomes {@code ****************1234}; short values are fully masked. */
    public static String mask(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString();
        if (s.length() <= 4) {
            return "*".repeat(s.length());
        }
        return "*".repeat(s.length() - 4) + s.substring(s.length() - 4);
    }
}
