package io.reconark.plugins.storagelocal;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Plugs in local-folder storage. Settings: {@code reconark.plugins.storage-local.root} (default {@code ./data/files});
 * switch off with {@code reconark.plugins.storage-local.enabled=false} when another storage plugin is used.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "reconark.plugins.storage-local.enabled", havingValue = "true", matchIfMissing = true)
public class LocalStoragePlugin {

    @Bean
    LocalFileStore localFileStore(@Value("${reconark.plugins.storage-local.root:./data/files}") String root) {
        return new LocalFileStore(Path.of(root));
    }
}
