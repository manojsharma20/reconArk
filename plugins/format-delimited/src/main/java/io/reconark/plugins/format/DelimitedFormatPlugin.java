package io.reconark.plugins.format;

import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.ExtensionPoints;

/** Contributes {@code format-reader:delimited}. */
public final class DelimitedFormatPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("format-delimited", "0.2.0")
            .name("Delimited format")
            .description("CSV, TSV, pipe-delimited and similar")
            .provides(ExtensionPoints.FORMAT_READER)
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        registrar.contribute(ExtensionPoints.FORMAT_READER, "delimited", new DelimitedFormatReader());
    }
}
