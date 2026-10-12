package io.reconark.common.plugin;

import io.reconark.common.model.ResultRow;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/** Writes reconciliation results to a downloadable file (CSV, Excel, PDF ...). */
public interface ReportWriter extends Plugin {

    /** Content type for the HTTP response, e.g. {@code text/csv}. */
    String contentType();

    /** File extension without the dot, e.g. {@code csv}. */
    String fileExtension();

    /**
     * Writes the rows to {@code out}. Must stream: {@code rows} may hold millions of results.
     *
     * @param fields business fields to show for each side, in display order
     */
    void write(List<String> fields, Iterator<ResultRow> rows, OutputStream out) throws IOException;
}
