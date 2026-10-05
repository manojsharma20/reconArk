package io.reconark.spi;

import io.reconark.kernel.api.PluginConfig;
import java.io.IOException;
import java.io.OutputStream;

/** Renders report data into a file format (CSV, XLSX, PDF ...). */
public interface ReportRenderer {

    /** Media type of the output, e.g. {@code text/csv}. */
    String mediaType();

    /** File extension without dot, e.g. {@code csv}. */
    String fileExtension();

    void render(ReportData data, PluginConfig options, OutputStream out) throws IOException;
}
