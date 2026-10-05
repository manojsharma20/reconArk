package io.reconark.spi;

/** reconArk's own recon outcome vocabulary (clean-room, ADR-0025). */
public enum OutcomeStatus {
    MATCHED,
    MATCHED_WITH_WARNINGS,
    MISMATCHED,
    UNMATCHED_LEFT,
    UNMATCHED_RIGHT,
    DUPLICATE,
    PENDING
}
