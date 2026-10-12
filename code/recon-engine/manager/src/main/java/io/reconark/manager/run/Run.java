package io.reconark.manager.run;

import io.reconark.common.model.ReconDefinition;
import java.time.Instant;
import java.util.UUID;

/** One reconciliation run, as stored in {@code engine.recon_run}. */
public record Run(
        UUID id,
        String definitionId,
        String definitionName,
        ReconDefinition definition,
        String leftFileId,
        String rightFileId,
        RunStatus status,
        int bucketCount,
        long leftLoaded,
        long rightLoaded,
        long rejected,
        long matched,
        long mismatched,
        long onlyInLeft,
        long onlyInRight,
        String error,
        Instant createdAt,
        Instant finishedAt) {}
