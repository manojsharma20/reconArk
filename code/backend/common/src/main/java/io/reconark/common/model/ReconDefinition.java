package io.reconark.common.model;

import java.util.List;

/**
 * Everything needed to reconcile two data sources: how to read each side, which fields identify a record (the match
 * key) and how to compare the other fields.
 *
 * <p>Example: "Bank statement vs. General ledger" matches on {@code reference}, and compares {@code amount} with a
 * tolerance of 0.01 and {@code valueDate} within 1 day.
 *
 * @param id            unique id, assigned by config-service
 * @param name          human-readable name shown in the UI and reports
 * @param left          the first data source (e.g. the bank statement)
 * @param right         the second data source (e.g. the ledger)
 * @param matchFields   fields that together identify the same transaction on both sides
 * @param compareRules  how to compare the remaining fields once two records are paired
 */
public record ReconDefinition(
        String id,
        String name,
        SourceSpec left,
        SourceSpec right,
        List<String> matchFields,
        List<CompareRule> compareRules) {

    public ReconDefinition {
        matchFields = matchFields == null ? List.of() : List.copyOf(matchFields);
        compareRules = compareRules == null ? List.of() : List.copyOf(compareRules);
    }

    public ReconDefinition withId(String newId) {
        return new ReconDefinition(newId, name, left, right, matchFields, compareRules);
    }

    /** All business fields used by this definition: match fields first, then compared fields. */
    public List<String> allFields() {
        var fields = new java.util.LinkedHashSet<>(matchFields);
        compareRules.forEach(r -> fields.add(r.field()));
        return List.copyOf(fields);
    }

    /** The source specification for one side. */
    public SourceSpec source(Side side) {
        return side == Side.LEFT ? left : right;
    }
}
