package io.reconark.kernel.pipeline;

import io.reconark.kernel.api.Cardinality;
import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.ExtensionPoint;

/**
 * One step of a pipeline. Contributed by plugins under a key (e.g. {@code parse}, {@code validate},
 * {@code pii-tokenize}) and placed in a pipeline by configuration.
 */
public interface PipelineStage {

    /** The {@code pipeline-stage} extension point. */
    ExtensionPoint<PipelineStage> POINT = ExtensionPoint.of("pipeline-stage", PipelineStage.class, Cardinality.KEYED);

    /** Options this stage accepts in a provider configuration; validated at save time and at compile time. */
    default ConfigSpec options() {
        return ConfigSpec.NONE;
    }

    /**
     * Transforms the payload. Record stages reject bad records with {@link RecordEnvelope#reject} and return the
     * rest; they must not drop records silently.
     */
    StagePayload apply(StageContext context, StagePayload input) throws Exception;
}
