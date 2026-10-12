package io.reconark.worker;

/** Counters reported back to the manager after a job. */
public record JobStats(long recordsRead, long recordsRejected, long resultsWritten) {}
