package io.reconark.plugins.fixedwidth;

import io.reconark.common.plugin.FileFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads fixed-width text: every column sits at a fixed position on the line.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code columns} (required): {@code name:start:length} entries separated by commas, start is 1-based.
 *       Example: {@code reference:1:10,amount:11:12,valueDate:23:8}</li>
 *   <li>{@code skipLines}: number of header lines to skip, default 0</li>
 *   <li>{@code encoding}: default {@code UTF-8}</li>
 * </ul>
 * Values are trimmed. A line shorter than a column's end gives the part that exists (or an empty value).
 */
public class FixedWidthFileFormat implements FileFormat {

    record Column(String name, int start, int length) {}

    @Override
    public String id() {
        return "fixed-width";
    }

    @Override
    public String description() {
        return "Fixed-width text. Options: columns (name:start:length,...), skipLines, encoding.";
    }

    @Override
    public void read(InputStream content, Map<String, String> options, Consumer<Map<String, String>> onRow)
            throws IOException {
        List<Column> columns = parseColumns(options.get("columns"));
        int skip = Integer.parseInt(options.getOrDefault("skipLines", "0"));
        var reader = new BufferedReader(new InputStreamReader(content, Charset.forName(options.getOrDefault("encoding", "UTF-8"))));

        String line;
        long lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (lineNumber <= skip || line.isBlank()) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (Column c : columns) {
                int from = Math.min(c.start() - 1, line.length());
                int to = Math.min(from + c.length(), line.length());
                row.put(c.name(), line.substring(from, to).trim());
            }
            onRow.accept(row);
        }
    }

    static List<Column> parseColumns(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("fixed-width: option 'columns' is required, e.g. reference:1:10,amount:11:12");
        }
        List<Column> columns = new ArrayList<>();
        for (String part : spec.split(",")) {
            String[] bits = part.trim().split(":");
            if (bits.length != 3) {
                throw new IllegalArgumentException("fixed-width: column '" + part.trim() + "' must be name:start:length");
            }
            int start = Integer.parseInt(bits[1].trim());
            int length = Integer.parseInt(bits[2].trim());
            if (start < 1 || length < 1) {
                throw new IllegalArgumentException("fixed-width: column '" + part.trim() + "' needs start >= 1 and length >= 1");
            }
            columns.add(new Column(bits[0].trim(), start, length));
        }
        return columns;
    }
}
