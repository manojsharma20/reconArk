package io.reconark.engine.messages;

import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.Side;
import java.util.UUID;

/**
 * One unit of work, sent by the manager to the workers. It carries everything the worker needs, so a worker never has
 * to call another service.
 *
 * @param jobId       unique id of the job
 * @param runId       the run this job belongs to (also the Kafka message key, so one run's messages stay in order)
 * @param type        LOAD or MATCH
 * @param attempt     1 for the first try, 2 for the first retry, ...
 * @param definition  the recon definition, as it was when the run started
 * @param bucketCount how many buckets the run uses
 * @param side        LOAD only: which side the file belongs to
 * @param storageKey  LOAD only: where the file is stored
 * @param bucket      MATCH only: which bucket to match (0 .. bucketCount-1)
 */
public record JobMessage(
        UUID jobId,
        UUID runId,
        JobType type,
        int attempt,
        ReconDefinition definition,
        int bucketCount,
        Side side,
        String storageKey,
        Integer bucket) {

    public static JobMessage load(UUID jobId, UUID runId, int attempt, ReconDefinition definition, int bucketCount,
            Side side, String storageKey) {
        return new JobMessage(jobId, runId, JobType.LOAD, attempt, definition, bucketCount, side, storageKey, null);
    }

    public static JobMessage match(UUID jobId, UUID runId, int attempt, ReconDefinition definition, int bucketCount,
            int bucket) {
        return new JobMessage(jobId, runId, JobType.MATCH, attempt, definition, bucketCount, null, null, bucket);
    }
}
