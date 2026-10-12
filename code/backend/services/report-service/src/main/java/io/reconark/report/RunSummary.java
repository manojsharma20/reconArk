package io.reconark.report;

import java.time.Instant;
import java.util.List;

/** The headline numbers of a run, for the report page. {@code fields} are the columns to show for each side. */
public record RunSummary(
        String runId,
        String definitionName,
        String status,
        long leftLoaded,
        long rightLoaded,
        long rejected,
        long matched,
        long mismatched,
        long onlyInLeft,
        long onlyInRight,
        List<String> fields,
        Instant createdAt,
        Instant finishedAt) {

    /** Share of matched keys among all keys, 0..100 (0 when there are no results yet). */
    public double matchRatePercent() {
        long total = matched + mismatched + onlyInLeft + onlyInRight;
        return total == 0 ? 0 : Math.round(matched * 10_000.0 / total) / 100.0;
    }
}
