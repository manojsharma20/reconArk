package io.reconark.plugins.storage.fs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.testing.tck.PluginContractTck;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemStoragePluginTest extends PluginContractTck {

    @TempDir
    Path tmp;

    @Override
    protected ReconArkPlugin plugin() {
        return new FilesystemStoragePlugin();
    }

    @Override
    protected Map<String, Object> configuration() {
        return Map.of("root", tmp.toString());
    }

    @Test
    void storesReadsRangesAndBlocksTraversal() throws Exception {
        FilesystemObjectStore store = new FilesystemObjectStore(tmp);
        String sha = store.put("raw/2026-10-01/a.csv", new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8)), Map.of());
        assertThat(sha).hasSize(64);
        try (var in = store.openRange("raw/2026-10-01/a.csv", 3, 4)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("3456");
        }
        assertThatThrownBy(() -> store.open("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    }
}
