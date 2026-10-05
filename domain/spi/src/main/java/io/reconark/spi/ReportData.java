package io.reconark.spi;

import java.util.List;

/**
 * Tabular report data, streamed row by row from a replica.
 *
 * @param title report title
 * @param columns column headers
 * @param rows rows; values already masked
 */
public record ReportData(String title, List<String> columns, Iterable<List<Object>> rows) {
    public ReportData {
        columns = List.copyOf(columns);
    }
}
