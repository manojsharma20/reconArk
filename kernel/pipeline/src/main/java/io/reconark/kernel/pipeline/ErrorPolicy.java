package io.reconark.kernel.pipeline;

/** What happens when a stage throws (record-level problems are always reported by rejecting the record). */
public enum ErrorPolicy {
    /** Default: the stage rejects bad records itself; an exception fails the unit of work, which is retried. */
    REJECT_RECORD,
    /** The source itself is suspect (checksum, decryption): hold every record of the unit and alert. */
    QUARANTINE_SOURCE,
    /** Configuration or infrastructure error: fail the run. */
    FAIL_RUN
}
