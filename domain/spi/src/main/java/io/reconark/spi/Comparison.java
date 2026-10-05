package io.reconark.spi;

/**
 * Result of comparing one field pair.
 *
 * @param equal whether the values are considered equal under the comparator's rules
 * @param explanation why not, in human-readable form (values masked by the engine where sensitive)
 */
public record Comparison(boolean equal, String explanation) {

    public static final Comparison EQUAL = new Comparison(true, "");

    public static Comparison different(String explanation) {
        return new Comparison(false, explanation);
    }
}
