package io.reconark.plugins.format;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.spi.FormatReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 4180-style reader with a configurable delimiter. Streams line by line (constant memory); quoted fields may
 * contain delimiters, doubled quotes and line breaks.
 */
final class DelimitedFormatReader implements FormatReader {

    @Override
    public ConfigSpec options() {
        return ConfigSpec.of(
                PropertySpec.optional("delimiter", PropertyType.STRING, ",", "single character; use \\t for tab"),
                PropertySpec.optional("header", PropertyType.BOOLEAN, true, "first row holds column names"),
                PropertySpec.optional("encoding", PropertyType.STRING, "UTF-8", "character set"),
                PropertySpec.optional("max-columns", PropertyType.INTEGER, 512, "defence against malformed input"));
    }

    @Override
    public void read(InputStream in, PluginConfig options, Sink sink) throws IOException {
        String d = options.string("delimiter");
        char delimiter = "\\t".equals(d) ? '\t' : d.charAt(0);
        boolean header = options.bool("header");
        int maxColumns = options.integer("max-columns");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(options.string("encoding"))))) {
            List<String> names = null;
            long position = 0;
            List<String> row;
            boolean first = true;
            while ((row = nextRow(reader, delimiter, maxColumns, first)) != null) {
                first = false;
                if (row.size() == 1 && row.getFirst().isEmpty()) {
                    continue; // blank line
                }
                if (header && names == null) {
                    names = row.stream().map(String::strip).toList();
                    continue;
                }
                position++;
                Map<String, Object> fields = new LinkedHashMap<>();
                for (int i = 0; i < row.size(); i++) {
                    String name = names != null && i < names.size() ? names.get(i) : "col" + (i + 1);
                    fields.put(name, row.get(i));
                }
                sink.accept(position, fields);
            }
        }
    }

    private static List<String> nextRow(BufferedReader reader, char delimiter, int maxColumns, boolean first) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        if (first && !line.isEmpty() && line.charAt(0) == '﻿') {
            line = line.substring(1);
        }
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        while (true) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (quoted) {
                    if (c == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            cur.append('"');
                            i++;
                        } else {
                            quoted = false;
                        }
                    } else {
                        cur.append(c);
                    }
                } else if (c == '"' && cur.isEmpty()) {
                    quoted = true;
                } else if (c == delimiter) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    if (out.size() > maxColumns) {
                        throw new IOException("Row exceeds max-columns " + maxColumns);
                    }
                } else {
                    cur.append(c);
                }
            }
            if (!quoted) {
                break;
            }
            String next = reader.readLine();
            if (next == null) {
                throw new IOException("Unterminated quoted field");
            }
            cur.append('\n');
            line = next;
        }
        out.add(cur.toString());
        return out;
    }
}
