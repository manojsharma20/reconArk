package io.reconark.common.model;

import java.util.List;
import java.util.Map;

/**
 * The reconciliation outcome for one match key, as stored in the database and shown in reports.
 *
 * @param left  the left record's business fields, or {@code null} when {@link MatchStatus#ONLY_IN_RIGHT}
 * @param right the right record's business fields, or {@code null} when {@link MatchStatus#ONLY_IN_LEFT}
 */
public record ResultRow(
        String matchKey,
        MatchStatus status,
        Map<String, String> left,
        Map<String, String> right,
        List<Difference> differences) {

    public ResultRow {
        differences = differences == null ? List.of() : List.copyOf(differences);
    }
}
