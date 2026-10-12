package io.reconark.manager.clients;

import io.reconark.common.model.Side;

/** The details of an uploaded file, as returned by ingestion-service. */
public record FileInfo(String id, String definitionId, Side side, String originalName, String storageKey, long sizeBytes) {}
