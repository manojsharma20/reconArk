package io.reconark.manager.run;

/** A job is QUEUED (sent to Kafka, waiting for an answer) until a worker reports DONE, or FAILED after the last try. */
public enum JobStatus {
    QUEUED,
    DONE,
    FAILED
}
