package io.reconark.plugins.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvFileFormatTest {

    private final CsvFileFormat csv = new CsvFileFormat();

    private List<Map<String, String>> read(String text, Map<String, String> options) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        csv.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), options, rows::add);
        return rows;
    }

    @Test
    void readsHeaderAndRows() throws IOException {
        var rows = read("ref,amount\r\nT1,10.00\r\nT2,20.50\r\n", Map.of());
        assertThat(rows).containsExactly(Map.of("ref", "T1", "amount", "10.00"), Map.of("ref", "T2", "amount", "20.50"));
    }

    @Test
    void handlesQuotesEscapedQuotesAndMultiLineValues() throws IOException {
        var rows = read("ref,note\nT1,\"a, b\"\nT2,\"say \"\"hi\"\"\"\nT3,\"two\nlines\"\n", Map.of());
        assertThat(rows).extracting(r -> r.get("note")).containsExactly("a, b", "say \"hi\"", "two\nlines");
    }

    @Test
    void supportsOtherDelimitersAndNoHeader() throws IOException {
        var rows = read("T1;10\n", Map.of("delimiter", ";", "header", "false", "columns", "ref, amount"));
        assertThat(rows).containsExactly(Map.of("ref", "T1", "amount", "10"));
    }

    @Test
    void skipsBlankLinesAndByteOrderMark() throws IOException {
        var rows = read("﻿ref\nT1\n\nT2\n", Map.of());
        assertThat(rows).extracting(r -> r.get("ref")).containsExactly("T1", "T2");
    }

    @Test
    void reportsTheLineOfAMalformedRow() {
        assertThatThrownBy(() -> read("a,b\n1,2\n3\n", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 3");
    }
}
