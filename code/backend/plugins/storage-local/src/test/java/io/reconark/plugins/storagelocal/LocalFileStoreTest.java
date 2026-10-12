package io.reconark.plugins.storagelocal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStoreTest {

    @TempDir
    Path root;

    @Test
    void savesAndOpensFiles() throws IOException {
        var store = new LocalFileStore(root);
        String key = store.save("bank statement.csv", new ByteArrayInputStream("ref\nT1\n".getBytes(StandardCharsets.UTF_8)));
        assertThat(key).endsWith("-bank_statement.csv");
        try (InputStream in = store.open(key)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("ref\nT1\n");
        }
    }

    @Test
    void refusesKeysOutsideTheRootFolder() {
        var store = new LocalFileStore(root);
        assertThatThrownBy(() -> store.open("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsFoldersAndOddCharactersFromNames() {
        assertThat(LocalFileStore.safeName("../x/y z.csv")).isEqualTo("y_z.csv");
    }
}
