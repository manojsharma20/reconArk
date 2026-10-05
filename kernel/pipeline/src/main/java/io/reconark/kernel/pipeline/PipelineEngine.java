package io.reconark.kernel.pipeline;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.PluginConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/** Compiles pipeline definitions against the active {@code pipeline-stage} extensions. Stateless and thread-safe. */
public final class PipelineEngine {

    private final ExtensionLookup extensions;
    private final BiFunction<String, PipelineStage, OptionsValidator> validatorFactory;

    /** Validates stage options; supplied by the kernel runtime so this module stays independent of it. */
    @FunctionalInterface
    public interface OptionsValidator {
        PluginConfig validate(Map<String, Object> rawOptions);
    }

    /**
     * @param extensions active extensions
     * @param validatorFactory creates an options validator for a stage key and stage (e.g. backed by
     *     {@code ConfigValidator.validate(key, stage.options(), raw)})
     */
    public PipelineEngine(ExtensionLookup extensions, BiFunction<String, PipelineStage, OptionsValidator> validatorFactory) {
        this.extensions = extensions;
        this.validatorFactory = validatorFactory;
    }

    /** Resolves every stage key and validates every stage's options; fails before any data moves. */
    public CompiledPipeline compile(PipelineDefinition definition) {
        List<CompiledStage> stages = new ArrayList<>();
        for (StageDefinition sd : definition.stages()) {
            PipelineStage stage = extensions.keyed(PipelineStage.POINT, sd.key());
            PluginConfig options = validatorFactory.apply(sd.key(), stage).validate(sd.options());
            stages.add(new CompiledStage(sd, stage, options));
        }
        return new CompiledPipeline(definition.id(), stages, extensions);
    }

    record CompiledStage(StageDefinition definition, PipelineStage stage, PluginConfig options) {}

    /** A ready-to-run pipeline. Reusable across units of work. */
    public static final class CompiledPipeline {
        private final String id;
        private final List<CompiledStage> stages;
        private final ExtensionLookup extensions;

        CompiledPipeline(String id, List<CompiledStage> stages, ExtensionLookup extensions) {
            this.id = id;
            this.stages = List.copyOf(stages);
            this.extensions = extensions;
        }

        public String id() {
            return id;
        }

        public List<String> stageKeys() {
            return stages.stream().map(s -> s.definition().key()).toList();
        }

        /** Runs one unit of work (a chunk, a pushed batch, a dry-run sample). */
        public PipelineResult run(String runId, Map<String, Object> attributes, StagePayload input) {
            StagePayload payload = input;
            List<RecordEnvelope> all = null;
            List<RecordEnvelope> rejected = new ArrayList<>();
            for (CompiledStage cs : stages) {
                StageContext ctx = new StageContext(runId, cs.definition().key(), cs.options(), extensions, attributes);
                try {
                    payload = cs.stage().apply(ctx, payload);
                } catch (Exception e) {
                    if (cs.definition().onError() == ErrorPolicy.QUARANTINE_SOURCE) {
                        List<RecordEnvelope> held = all == null ? List.of() : all;
                        held.forEach(RecordEnvelope::quarantine);
                        List<RecordEnvelope> heldNow = held.stream().filter(r -> r.disposition() == RecordEnvelope.Disposition.QUARANTINED).toList();
                        List<RecordEnvelope> rejectedNow = held.stream().filter(r -> r.disposition() == RecordEnvelope.Disposition.REJECTED).toList();
                        return new PipelineResult(held.size(), List.of(), rejectedNow, heldNow,
                                Optional.of(cs.definition().key() + ": " + e.getClass().getSimpleName()));
                    }
                    throw new PipelineFailure(cs.definition().key(), "Stage '" + cs.definition().key() + "' failed in pipeline '"
                            + id + "'", e);
                }
                if (payload instanceof StagePayload.Records(List<RecordEnvelope> records)) {
                    if (all == null) {
                        all = records; // first record-producing stage defines 'read'
                    }
                    List<RecordEnvelope> pending = new ArrayList<>(records.size());
                    for (RecordEnvelope r : records) {
                        if (r.pending()) {
                            pending.add(r);
                        } else if (r.disposition() == RecordEnvelope.Disposition.REJECTED) {
                            rejected.add(r);
                        }
                    }
                    payload = new StagePayload.Records(pending);
                }
            }
            if (!(payload instanceof StagePayload.Records(List<RecordEnvelope> remaining))) {
                throw new PipelineFailure("<end>", "Pipeline '" + id + "' never produced records; add a parse stage", null);
            }
            remaining.forEach(r -> r.settle(RecordEnvelope.Disposition.ACCEPTED));
            return new PipelineResult(all.size(), remaining, rejected, List.of(), Optional.empty());
        }
    }
}
