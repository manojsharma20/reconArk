package io.reconark.plugins.report.csv;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.spi.ReportData;
import io.reconark.spi.ReportRenderer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Renders RFC 4180 CSV. Text cells starting with {@code = + - @ TAB CR} get a leading {@code '} (CSV injection). */
final class CsvReportRenderer implements ReportRenderer {

    @Override
    public String mediaType() {
        return "text/csv";
    }

    @Override
    public String fileExtension() {
        return "csv";
    }

    @Override
    public void render(ReportData data, PluginConfig options, OutputStream out) throws IOException {
        Writer w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writeRow(w, data.columns());
        for (List<Object> row : data.rows()) {
            writeRow(w, row);
        }
        w.flush();
    }

    private static void writeRow(Writer w, List<?> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(cell(cells.get(i)));
        }
        w.write("\r\n");
    }

    static String cell(Object v) {
        if (v == null) {
            return "";
        }
        String s = v.toString();
        if (!(v instanceof Number) && !s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            s = '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
