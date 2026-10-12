package io.reconark.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SourceSpecTest {

    @Test
    void mapsColumnsToBusinessFieldsAndKeepsUnmappedColumns() {
        var spec = new SourceSpec("Bank", "csv", Map.of(), Map.of("amount", "AMT", "reference", "REF"));
        var fields = spec.toBusinessFields(Map.of("AMT", "10.00", "REF", "T1", "BRANCH", "DXB"));
        assertThat(fields).containsOnly(
                Map.entry("amount", "10.00"), Map.entry("reference", "T1"), Map.entry("BRANCH", "DXB"));
    }
}
