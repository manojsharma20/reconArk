package io.reconark.plugins.fixedwidth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FixedWidthFileFormatTest {

    private final FixedWidthFileFormat format = new FixedWidthFileFormat();

    @Test
    void cutsColumnsAtTheirPositions() throws IOException {
        String text = "HEADER LINE\nT1        0000010.0020261001\nT2        0000020.5020261002\n";
        List<Map<String, String>> rows = new ArrayList<>();
        format.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                Map.of("columns", "reference:1:10, amount:11:10, valueDate:21:8", "skipLines", "1"), rows::add);
        assertThat(rows).containsExactly(
                Map.of("reference", "T1", "amount", "0000010.00", "valueDate", "20261001"),
                Map.of("reference", "T2", "amount", "0000020.50", "valueDate", "20261002"));
    }

    @Test
    void shortLinesGiveEmptyValues() throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        format.read(new ByteArrayInputStream("AB\n".getBytes(StandardCharsets.UTF_8)),
                Map.of("columns", "a:1:2,b:3:5"), rows::add);
        assertThat(rows).containsExactly(Map.of("a", "AB", "b", ""));
    }

    @Test
    void columnsOptionIsRequired() {
        assertThatThrownBy(() -> format.read(new ByteArrayInputStream(new byte[0]), Map.of(), r -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("columns");
    }
}
