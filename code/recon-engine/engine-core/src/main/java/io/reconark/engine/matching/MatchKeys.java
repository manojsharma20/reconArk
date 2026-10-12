package io.reconark.engine.matching;

import java.util.List;
import java.util.Map;

/**
 * Builds the match key of a record and decides which bucket it belongs to.
 *
 * <p>The match key joins the trimmed values of the match fields with {@code |}. Example: match fields
 * {@code [reference, currency]} and values {@code T1, AED} give the key {@code T1|AED}.
 *
 * <p>Buckets split a run into independent pieces of work: the same key always lands in the same bucket, so each
 * bucket can be matched by a different worker in parallel.
 */
public final class MatchKeys {

    private MatchKeys() {}

    /** Returns the match key, or {@code null} when any match field is missing or blank (the row is rejected). */
    public static String build(Map<String, String> fields, List<String> matchFields) {
        var key = new StringBuilder();
        for (String field : matchFields) {
            String value = fields.get(field);
            if (value == null || value.isBlank()) {
                return null;
            }
            if (!key.isEmpty()) {
                key.append('|');
            }
            key.append(value.trim());
        }
        return key.toString();
    }

    /** The bucket for a key: a number from 0 to {@code bucketCount - 1}, always the same for the same key. */
    public static int bucket(String matchKey, int bucketCount) {
        return Math.floorMod(matchKey.hashCode(), bucketCount);
    }
}
