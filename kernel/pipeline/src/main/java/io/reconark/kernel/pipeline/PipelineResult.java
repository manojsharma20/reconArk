package io.reconark.kernel.pipeline;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of running a pipeline over one unit of work. The constructor enforces the accounting invariant.
 *
 * @param read records produced by parsing
 * @param accepted records that passed every stage
 * @param rejected records rejected with violations
 * @param quarantined records held because the source is suspect
 * @param quarantineReason why the source was quarantined, if it was
 */
public record PipelineResult(
        long read,
        List<RecordEnvelope> accepted,
        List<RecordEnvelope> rejected,
        List<RecordEnvelope> quarantined,
        Optional<String> quarantineReason) {

    public PipelineResult {
        accepted = List.copyOf(accepted);
        rejected = List.copyOf(rejected);
        quarantined = List.copyOf(quarantined);
        if (read != (long) accepted.size() + rejected.size() + quarantined.size()) {
            throw new IllegalStateException("Record accounting broken: read=" + read + " accepted=" + accepted.size()
                    + " rejected=" + rejected.size() + " quarantined=" + quarantined.size());
        }
    }
}
