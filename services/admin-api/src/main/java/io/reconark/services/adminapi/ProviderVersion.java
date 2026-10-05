package io.reconark.services.adminapi;

import io.reconark.kernel.pipeline.StageDefinition;
import java.time.Instant;
import java.util.List;

/**
 * One immutable provider configuration version (build prompt §6). The skeleton keeps versions in memory; the
 * persistence adapter stores them in {@code config.provider_config_version} with a content hash.
 *
 * @param providerCode provider code
 * @param version version number
 * @param description change description
 * @param pipeline ETL stage graph
 * @param state lifecycle state
 * @param maker who created it
 * @param checker who approved it, if approved
 * @param createdAt creation time
 */
record ProviderVersion(
        String providerCode,
        int version,
        String description,
        List<StageDefinition> pipeline,
        State state,
        String maker,
        String checker,
        Instant createdAt) {

    /** Version lifecycle (diagram 10-config-version-lifecycle). */
    enum State { DRAFT, SUBMITTED, APPROVED, REJECTED }

    ProviderVersion with(State next, String approver) {
        return new ProviderVersion(providerCode, version, description, pipeline, next, maker, approver, createdAt);
    }
}
