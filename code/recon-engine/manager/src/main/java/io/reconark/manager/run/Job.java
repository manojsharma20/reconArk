package io.reconark.manager.run;

import io.reconark.common.model.Side;
import io.reconark.engine.messages.JobType;
import java.time.Instant;
import java.util.UUID;

/** One job of a run, as stored in {@code engine.recon_job}. */
public record Job(
        UUID id,
        UUID runId,
        JobType type,
        Side side,
        String storageKey,
        Integer bucket,
        JobStatus status,
        int attempts,
        long recordsRead,
        long recordsRejected,
        long resultsWritten,
        String workerId,
        String error,
        Instant sentAt,
        Instant finishedAt) {}
