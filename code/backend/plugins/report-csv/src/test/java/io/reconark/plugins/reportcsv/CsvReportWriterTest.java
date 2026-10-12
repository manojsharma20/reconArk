package io.reconark.plugins.reportcsv;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.common.model.Difference;
import io.reconark.common.model.MatchStatus;
import io.reconark.common.model.ResultRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvReportWriterTest {

    @Test
    void writesOneLinePerResultWithBothSides() throws IOException {
        var rows = List.of(
                new ResultRow("T1", MatchStatus.MISMATCHED, Map.of("ref", "T1", "amount", "10.00"),
                        Map.of("ref", "T1", "amount", "12.00"), List.of(new Difference("amount", "10.00", "12.00", "number"))),
                new ResultRow("T2", MatchStatus.ONLY_IN_LEFT, Map.of("ref", "T2", "amount", "5"), null, List.of()));
        var out = new ByteArrayOutputStream();
        new CsvReportWriter().write(List.of("ref", "amount"), rows.iterator(), out);

        assertThat(out.toString(StandardCharsets.UTF_8).split("\r\n")).containsExactly(
                "match_key,status,differences,left_ref,left_amount,right_ref,right_amount",
                "T1,MISMATCHED,amount: 10.00 vs 12.00,T1,10.00,T1,12.00",
                "T2,ONLY_IN_LEFT,,T2,5,,");
    }

    @Test
    void escapesCommasQuotesAndFormulas() {
        assertThat(CsvReportWriter.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvReportWriter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvReportWriter.escape("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(CsvReportWriter.escape("-12.50")).isEqualTo("-12.50");
    }
}
