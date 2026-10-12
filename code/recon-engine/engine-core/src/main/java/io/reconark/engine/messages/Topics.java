package io.reconark.engine.messages;

/** Kafka topic names. Naming: {@code reconark.<area>.<what>.v<version>}. */
public final class Topics {

    /** Manager -> workers: work to do. All workers share one consumer group, so each job goes to one worker. */
    public static final String JOBS = "reconark.engine.jobs.v1";

    /** Workers -> manager: the outcome of each job. */
    public static final String JOB_RESULTS = "reconark.engine.job-results.v1";

    private Topics() {}
}
