package io.reconark.plugins.reportcsv;

import io.reconark.common.model.Difference;
import io.reconark.common.model.ResultRow;
import io.reconark.common.plugin.ReportWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One CSV line per result: {@code match_key, status, differences, left_<field>..., right_<field>...}.
 *
 * <p>Values that start with {@code = + - @} are prefixed with a quote so spreadsheet tools don't run them as formulas
 * (CSV injection).
 */
public class CsvReportWriter implements ReportWriter {

    @Override
    public String id() {
        return "csv";
    }

    @Override
    public String description() {
        return "Results as CSV: one line per match key, both sides side by side.";
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public String fileExtension() {
        return "csv";
    }

    @Override
    public void write(List<String> fields, Iterator<ResultRow> rows, OutputStream out) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        List<String> header = new ArrayList<>(List.of("match_key", "status", "differences"));
        fields.forEach(f -> header.add("left_" + f));
        fields.forEach(f -> header.add("right_" + f));
        writeLine(writer, header);

        while (rows.hasNext()) {
            ResultRow row = rows.next();
            List<String> line = new ArrayList<>();
            line.add(row.matchKey());
            line.add(row.status().name());
            line.add(describe(row.differences()));
            fields.forEach(f -> line.add(value(row.left(), f)));
            fields.forEach(f -> line.add(value(row.right(), f)));
            writeLine(writer, line);
        }
        writer.flush();
    }

    private static String describe(List<Difference> differences) {
        return differences.stream()
                .map(d -> d.field() + ": " + d.leftValue() + " vs " + d.rightValue())
                .collect(Collectors.joining("; "));
    }

    private static String value(Map<String, String> record, String field) {
        if (record == null) {
            return "";
        }
        String value = record.get(field);
        return value == null ? "" : value;
    }

    private static void writeLine(BufferedWriter writer, List<String> values) throws IOException {
        writer.write(values.stream().map(CsvReportWriter::escape).collect(Collectors.joining(",")));
        writer.write("\r\n");
    }

    static String escape(String value) {
        String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 && !looksLikeNumber(value)
                ? "'" + value
                : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static boolean looksLikeNumber(String value) {
        return value.matches("[+-]?\\d+(\\.\\d+)?");
    }
}
