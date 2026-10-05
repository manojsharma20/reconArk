package io.reconark.plugins.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.runtime.ConfigValidator;
import io.reconark.spi.FormatReader;
import io.reconark.testing.tck.FormatReaderTck;
import io.reconark.testing.tck.PluginContractTck;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DelimitedFormatPluginTest extends PluginContractTck {

    @Override
    protected ReconArkPlugin plugin() {
        return new DelimitedFormatPlugin();
    }

    @Nested
    class Tck extends FormatReaderTck {
        @Override
        protected FormatReader reader() {
            return new DelimitedFormatReader();
        }

        @Override
        protected byte[] document(int records) {
            StringBuilder sb = new StringBuilder("idx,name\n");
            for (int i = 1; i <= records; i++) {
                sb.append(i).append(",\"n, ").append(i).append("\"\n");
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected String indexField() {
            return "idx";
        }
    }

    @Test
    void handlesQuotesEmbeddedNewlinesBomAndPipes() throws Exception {
        String doc = "﻿a|b\n\"x \"\"q\"\"\"|\"line1\nline2\"\n";
        List<Map<String, Object>> rows = new ArrayList<>();
        PluginConfig opts = ConfigValidator.validate("t", new DelimitedFormatReader().options(), Map.of("delimiter", "|"));
        new DelimitedFormatReader().read(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)), opts, (p, f) -> rows.add(f));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("a", "x \"q\"").containsEntry("b", "line1\nline2");
    }
}
