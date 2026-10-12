package io.reconark.manager.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.reconark.common.model.Side;

/** The details of an uploaded file, as returned by ingestion-service (fields the manager doesn't need are ignored). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileInfo(String id, String definitionId, Side side, String originalName, String storageKey, long sizeBytes) {}
