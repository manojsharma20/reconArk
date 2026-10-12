package io.reconark.worker;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.common.model.CompareRule;
import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.Side;
import io.reconark.common.model.SourceSpec;
import io.reconark.common.plugin.FileStore;
import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.JobResultMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs real LOAD and MATCH jobs (real plugins, real PostgreSQL) without Kafka: the test calls {@link JobHandler}
 * directly, exactly as {@link JobListener} does.
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.flyway.schemas=engine",
        "spring.flyway.locations=classpath:db/migration/engine"
})
@Import(WorkerJobsTest.Containers.class)
class WorkerJobsTest {

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

    static final int BUCKETS = 4;

    static final ReconDefinition DEFINITION = new ReconDefinition("def-1", "Bank vs Ledger",
            new SourceSpec("Bank", "csv", Map.of(), Map.of("reference", "REF", "amount", "AMT")),
            new SourceSpec("Ledger", "csv", Map.of("delimiter", ";"), Map.of()),
            List.of("reference"),
            List.of(new CompareRule("amount", "number", Map.of("tolerance", "0.01"))));

    @Autowired
    JobHandler handler;

    @Autowired
    FileStore files;

    @Autowired
    JdbcClient jdbc;

    @Test
    void loadsBothFilesAndMatchesEveryBucket() throws IOException {
        String bank = save("REF,AMT\nT1,10.00\nT2,20.00\nT3,30.00\n,99.00\n");      // last row has no reference
        String ledger = save("reference;amount\nT1;10.005\nT2;25.00\nT4;40.00\n");
        UUID runId = UUID.randomUUID();

        JobMessage loadLeft = JobMessage.load(UUID.randomUUID(), runId, 1, DEFINITION, BUCKETS, Side.LEFT, bank);
        JobResultMessage left = handler.handle(loadLeft);
        assertThat(left.success()).isTrue();
        assertThat(left.recordsRead()).isEqualTo(4);
        assertThat(left.recordsRejected()).isEqualTo(1);

        assertThat(handler.handle(JobMessage.load(UUID.randomUUID(), runId, 1, DEFINITION, BUCKETS, Side.RIGHT, ledger))
                .success()).isTrue();

        // A retry of the same LOAD job must not duplicate records.
        handler.handle(loadLeft);
        assertThat(count("select count(*) from engine.recon_record where run_id = :run", runId)).isEqualTo(6);

        for (int bucket = 0; bucket < BUCKETS; bucket++) {
            JobMessage match = JobMessage.match(UUID.randomUUID(), runId, 1, DEFINITION, BUCKETS, bucket);
            assertThat(handler.handle(match).success()).isTrue();
            handler.handle(match); // retry: results are replaced, not duplicated
        }

        Map<String, String> statusByKey = jdbc.sql("select match_key, status from engine.recon_result where run_id = :run")
                .param("run", runId)
                .query((rs, row) -> Map.entry(rs.getString(1), rs.getString(2)))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(statusByKey).containsOnly(
                Map.entry("T1", "MATCHED"),
                Map.entry("T2", "MISMATCHED"),
                Map.entry("T3", "ONLY_IN_LEFT"),
                Map.entry("T4", "ONLY_IN_RIGHT"));
    }

    @Test
    void unknownFormatFailsTheJobWithAClearMessage() {
        var broken = new ReconDefinition("def-2", "Broken",
                new SourceSpec("A", "xml", Map.of(), Map.of()), new SourceSpec("B", "csv", Map.of(), Map.of()),
                List.of("ref"), List.of());
        JobResultMessage result = handler.handle(
                JobMessage.load(UUID.randomUUID(), UUID.randomUUID(), 1, broken, BUCKETS, Side.LEFT, "none"));
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Unknown format plugin 'xml'");
    }

    private String save(String content) throws IOException {
        return files.save("test.csv", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private long count(String sql, UUID runId) {
        return jdbc.sql(sql).param("run", runId).query(Long.class).single();
    }
}
