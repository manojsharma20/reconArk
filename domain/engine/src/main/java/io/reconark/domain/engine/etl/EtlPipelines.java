package io.reconark.domain.engine.etl;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.pipeline.PipelineEngine;
import io.reconark.kernel.runtime.ConfigValidator;

/** Factory for a {@link PipelineEngine} whose stage options are validated by the kernel's {@link ConfigValidator}. */
public final class EtlPipelines {

    private EtlPipelines() {}

    public static PipelineEngine engine(ExtensionLookup extensions) {
        return new PipelineEngine(extensions, (key, stage) -> raw -> ConfigValidator.validate("stage:" + key, stage.options(), raw));
    }
}
