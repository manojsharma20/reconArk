package io.reconark.engine.messages;

import java.util.UUID;

/**
 * What a worker reports back after a job, success or failure.
 *
 * @param attempt        copied from the {@link JobMessage}; lets the manager ignore late answers to an old attempt
 * @param recordsRead    LOAD: rows read from the file; MATCH: records read from the bucket
 * @param recordsRejected LOAD: rows skipped because a match field was empty
 * @param resultsWritten MATCH: result rows written
 * @param error          a short, safe description when {@code success} is false
 */
public record JobResultMessage(
        UUID jobId,
        UUID runId,
        JobType type,
        int attempt,
        boolean success,
        long recordsRead,
        long recordsRejected,
        long resultsWritten,
        String workerId,
        long durationMillis,
        String error) {}
