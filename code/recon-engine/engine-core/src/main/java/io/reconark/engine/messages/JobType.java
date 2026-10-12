package io.reconark.engine.messages;

/** The two kinds of work in a run. */
public enum JobType {
    /** Read one uploaded file, turn rows into records and store them, grouped into buckets by match key. */
    LOAD,
    /** Match the left and right records of one bucket and store the results. */
    MATCH
}
