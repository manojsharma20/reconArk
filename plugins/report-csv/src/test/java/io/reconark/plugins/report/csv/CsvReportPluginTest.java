package io.reconark.plugins.report.csv;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.ReportData;
import io.reconark.testing.tck.PluginContractTck;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvReportPluginTest extends PluginContractTck {

    @Override
    protected ReconArkPlugin plugin() {
        return new CsvReportPlugin();
    }

    @Test
    void neutralisesFormulaInjectionAndQuotes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<Object>> rows = List.of(Arrays.asList("=HYPERLINK(\"x\")", -5, "a,b", null));
        new CsvReportRenderer().render(new ReportData("t", List.of("c1", "c2", "c3", "c4"), rows), PluginConfig.EMPTY, out);
        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo("c1,c2,c3,c4\r\n\"'=HYPERLINK(\"\"x\"\")\",-5,\"a,b\",\r\n");
    }
}
