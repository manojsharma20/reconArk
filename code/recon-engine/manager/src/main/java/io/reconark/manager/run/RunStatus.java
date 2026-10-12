package io.reconark.manager.run;

/** The life of a run: LOADING -> MATCHING -> COMPLETED, or FAILED at any point. */
public enum RunStatus {
    /** LOAD jobs are reading the two files. */
    LOADING,
    /** MATCH jobs are matching the buckets. */
    MATCHING,
    /** All done; the results are ready in report-service. */
    COMPLETED,
    /** A job failed too often; see the run's error. */
    FAILED;

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED;
    }
}
