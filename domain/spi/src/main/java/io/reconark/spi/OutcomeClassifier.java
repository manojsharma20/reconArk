package io.reconark.spi;

import java.util.List;

/** Turns a group and its diffs into an outcome status. Replaceable when a bank needs its own semantics. */
public interface OutcomeClassifier {

    OutcomeStatus classify(MatchGroup group, List<FieldDiff> diffs);
}
