package io.reconark.ingestion;

import io.reconark.common.model.Side;
import java.time.Instant;

/**
 * An uploaded file.
 *
 * @param storageKey where the {@link io.reconark.common.plugin.FileStore} keeps it
 * @param sha256     fingerprint of the content: the same file uploaded twice has the same value
 */
public record UploadedFile(
        String id,
        String definitionId,
        Side side,
        String originalName,
        String storageKey,
        long sizeBytes,
        String sha256,
        Instant uploadedAt) {}
