package io.reconark.testing.tck;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.runtime.ConfigValidator;
import io.reconark.spi.FormatReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Contract of {@code format-reader}. */
public abstract class FormatReaderTck {

    protected abstract FormatReader reader();

    protected Map<String, Object> options() {
        return Map.of();
    }

    /** A valid document with exactly {@code records} records, each having a field holding its 1-based index. */
    protected abstract byte[] document(int records);

    /** Name of the field holding the record index in {@link #document}. */
    protected abstract String indexField();

    private List<Map<String, Object>> read(byte[] doc, List<Long> positions) throws Exception {
        PluginConfig opts = ConfigValidator.validate("tck", reader().options(), options());
        List<Map<String, Object>> out = new ArrayList<>();
        reader().read(new ByteArrayInputStream(doc), opts, (pos, fields) -> {
            positions.add(pos);
            out.add(fields);
        });
        return out;
    }

    @Test
    void readsEveryRecordInOrder() throws Exception {
        List<Long> positions = new ArrayList<>();
        List<Map<String, Object>> records = read(document(25), positions);
        assertThat(records).hasSize(25);
        assertThat(positions).isSorted().doesNotHaveDuplicates().startsWith(1L);
        for (int i = 0; i < records.size(); i++) {
            assertThat(String.valueOf(records.get(i).get(indexField()))).isEqualTo(String.valueOf(i + 1));
        }
    }

    @Test
    void emptyDocumentHasNoRecords() throws Exception {
        assertThat(read(document(0), new ArrayList<>())).isEmpty();
    }

    @Test
    void largeDocumentsStream() throws Exception {
        assertThat(read(document(20_000), new ArrayList<>())).hasSize(20_000);
    }
}
