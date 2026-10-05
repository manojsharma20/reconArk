package io.reconark.domain.engine.etl;

import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.ExtensionPoints;

/** The standard stages: {@code parse}, {@code map}, {@code validate}, {@code canonicalize}, plus the {@code required} validator. */
public final class CoreStagesPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("core-stages", "0.2.0")
            .name("Core ETL stages")
            .description("parse, map, validate, canonicalize stages and the 'required' validator")
            .provides(ExtensionPoints.PIPELINE_STAGE, ExtensionPoints.RECORD_VALIDATOR)
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        registrar.contribute(ExtensionPoints.PIPELINE_STAGE, "parse", new ParseStage());
        registrar.contribute(ExtensionPoints.PIPELINE_STAGE, "map", new MapStage());
        registrar.contribute(ExtensionPoints.PIPELINE_STAGE, "validate", new ValidateStage());
        registrar.contribute(ExtensionPoints.PIPELINE_STAGE, "canonicalize", new CanonicalizeStage());
        registrar.contribute(ExtensionPoints.RECORD_VALIDATOR, "required", new RequiredFieldsValidator());
    }
}
