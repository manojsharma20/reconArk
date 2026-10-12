package io.reconark.common.model;

/** The outcome for one match key. */
public enum MatchStatus {
    /** Found on both sides and every compared field is equal (within its rule). */
    MATCHED,
    /** Found on both sides but at least one compared field differs. */
    MISMATCHED,
    /** Found only on the left side (e.g. in the bank statement but not in the ledger). */
    ONLY_IN_LEFT,
    /** Found only on the right side. */
    ONLY_IN_RIGHT
}
