package io.reconark.plugins.csv;

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
 * Reads delimited text files (RFC 4180 CSV): quoted values, doubled quotes inside quotes, values that span lines.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code delimiter}: one character, default {@code ,} ({@code tab} for tab-separated)</li>
 *   <li>{@code header}: {@code true} (default) when the first line holds the column names</li>
 *   <li>{@code columns}: comma-separated column names, required when {@code header=false}</li>
 *   <li>{@code encoding}: default {@code UTF-8}</li>
 * </ul>
 */
public class CsvFileFormat implements FileFormat {

    @Override
    public String id() {
        return "csv";
    }

    @Override
    public String description() {
        return "Delimited text (CSV). Options: delimiter, header, columns, encoding.";
    }

    @Override
    public void read(InputStream content, Map<String, String> options, Consumer<Map<String, String>> onRow)
            throws IOException {
        char delimiter = delimiter(options.getOrDefault("delimiter", ","));
        boolean header = Boolean.parseBoolean(options.getOrDefault("header", "true"));
        Charset charset = Charset.forName(options.getOrDefault("encoding", "UTF-8"));

        var parser = new RecordParser(new BufferedReader(new InputStreamReader(content, charset)), delimiter);

        List<String> columns;
        if (header) {
            columns = parser.next();
            if (columns == null) {
                return; // empty file
            }
            columns = columns.stream().map(String::trim).toList();
        } else {
            String configured = options.get("columns");
            if (configured == null || configured.isBlank()) {
                throw new IllegalArgumentException("csv: option 'columns' is required when header=false");
            }
            columns = List.of(configured.trim().split("\\s*,\\s*"));
        }

        List<String> values;
        while ((values = parser.next()) != null) {
            if (values.size() == 1 && values.getFirst().isEmpty()) {
                continue; // blank line
            }
            if (values.size() != columns.size()) {
                throw new IllegalArgumentException("csv: line " + parser.lineNumber() + " has " + values.size()
                        + " values but the header has " + columns.size() + " columns");
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                row.put(columns.get(i), values.get(i));
            }
            onRow.accept(row);
        }
    }

    private static char delimiter(String value) {
        if ("\\t".equals(value) || "tab".equalsIgnoreCase(value)) {
            return '\t';
        }
        if (value.length() != 1) {
            throw new IllegalArgumentException("csv: delimiter must be one character, got '" + value + "'");
        }
        return value.charAt(0);
    }

    /** Splits the input into records; each call to {@link #next()} returns one record's values. */
    private static final class RecordParser {
        private final BufferedReader in;
        private final char delimiter;
        private long line;
        private boolean first = true;

        RecordParser(BufferedReader in, char delimiter) {
            this.in = in;
            this.delimiter = delimiter;
        }

        /** Line number where the most recently returned record ended. */
        long lineNumber() {
            return line;
        }

        /** Returns the next record's values, or {@code null} at the end of the input. */
        List<String> next() throws IOException {
            int c = in.read();
            if (first && c == '﻿') { // skip a byte-order mark
                c = in.read();
            }
            first = false;
            if (c == -1) {
                return null;
            }
            line++;
            List<String> values = new ArrayList<>();
            var value = new StringBuilder();
            boolean quoted = false;
            while (true) {
                if (c == -1) {
                    if (quoted) {
                        throw new IllegalArgumentException("csv: line " + line + " has an unclosed quote");
                    }
                    values.add(value.toString());
                    return values;
                }
                char ch = (char) c;
                if (quoted) {
                    if (ch == '"') {
                        in.mark(1);
                        if (in.read() == '"') {
                            value.append('"'); // "" inside quotes is one quote
                        } else {
                            quoted = false;
                            in.reset();
                        }
                    } else {
                        if (ch == '\n') {
                            line++;
                        }
                        value.append(ch);
                    }
                } else if (ch == '"' && value.isEmpty()) {
                    quoted = true;
                } else if (ch == delimiter) {
                    values.add(value.toString());
                    value.setLength(0);
                } else if (ch == '\n') {
                    values.add(value.toString());
                    return values;
                } else if (ch != '\r') {
                    value.append(ch);
                }
                c = in.read();
            }
        }
    }
}
