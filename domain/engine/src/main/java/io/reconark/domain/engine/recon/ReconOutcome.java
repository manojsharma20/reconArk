package io.reconark.domain.engine.recon;

import io.reconark.spi.FieldDiff;
import io.reconark.spi.OutcomeStatus;
import java.util.List;

/**
 * One evaluated group.
 *
 * @param matchKey normalised key
 * @param status outcome
 * @param leftKeys record keys on the left
 * @param rightKeys record keys on the right
 * @param diffs explained differences
 */
public record ReconOutcome(
        String matchKey, OutcomeStatus status, List<String> leftKeys, List<String> rightKeys, List<FieldDiff> diffs) {
    public ReconOutcome {
        leftKeys = List.copyOf(leftKeys);
        rightKeys = List.copyOf(rightKeys);
        diffs = List.copyOf(diffs);
    }
}
