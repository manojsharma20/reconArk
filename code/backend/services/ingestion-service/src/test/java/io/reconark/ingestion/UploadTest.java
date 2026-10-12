package io.reconark.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.common.model.Side;
import io.reconark.common.plugin.FileStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Import(UploadTest.Containers.class)
class UploadTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        Path root = Files.createTempDirectory("reconark-files");
        registry.add("reconark.plugins.storage-local.root", root::toString);
    }

    @Autowired
    FileController files;

    @Autowired
    FileStore store;

    @Test
    void uploadStoresTheFileAndItsDetails() throws IOException {
        byte[] content = "ref,amount\nT1,10.00\n".getBytes(StandardCharsets.UTF_8);
        UploadedFile uploaded = files.upload(new MockMultipartFile("file", "bank.csv", "text/csv", content), "def-1", Side.LEFT);

        assertThat(uploaded.sizeBytes()).isEqualTo(content.length);
        assertThat(uploaded.sha256()).hasSize(64);
        assertThat(uploaded.uploadedAt()).isNotNull();
        try (InputStream in = store.open(uploaded.storageKey())) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }
        assertThat(files.one(uploaded.id())).isEqualTo(uploaded);
        assertThat(files.list("def-1", Side.LEFT)).extracting(UploadedFile::id).contains(uploaded.id());
        assertThat(files.list("def-1", Side.RIGHT)).isEmpty();
        assertThat(files.list(null, null)).isNotEmpty();
    }
}
