package io.reconark.plugins.recon;

import io.reconark.spi.FieldDiff;
import io.reconark.spi.MatchGroup;
import io.reconark.spi.OutcomeClassifier;
import io.reconark.spi.OutcomeStatus;
import java.util.List;

/** {@code standard}: duplicates, then one-sided, then blocking diffs, then warnings. */
final class StandardOutcomeClassifier implements OutcomeClassifier {

    @Override
    public OutcomeStatus classify(MatchGroup group, List<FieldDiff> diffs) {
        if (group.left().size() > 1 || group.right().size() > 1) {
            return OutcomeStatus.DUPLICATE;
        }
        if (group.right().isEmpty()) {
            return OutcomeStatus.UNMATCHED_LEFT;
        }
        if (group.left().isEmpty()) {
            return OutcomeStatus.UNMATCHED_RIGHT;
        }
        if (diffs.stream().anyMatch(d -> d.severity() == FieldDiff.Severity.BLOCKING)) {
            return OutcomeStatus.MISMATCHED;
        }
        return diffs.isEmpty() ? OutcomeStatus.MATCHED : OutcomeStatus.MATCHED_WITH_WARNINGS;
    }
}
