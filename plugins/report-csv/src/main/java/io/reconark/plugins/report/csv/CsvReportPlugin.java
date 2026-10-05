package io.reconark.plugins.report.csv;

import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.ExtensionPoints;

/** Contributes {@code report-renderer:csv}. */
public final class CsvReportPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("report-csv", "0.2.0")
            .name("CSV reports")
            .description("CSV renderer with formula-injection protection")
            .provides(ExtensionPoints.REPORT_RENDERER)
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        registrar.contribute(ExtensionPoints.REPORT_RENDERER, "csv", new CsvReportRenderer());
    }
}
