package io.reconark.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reconark.common.model.MatchStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "spring.flyway.schemas=engine",
        "spring.flyway.locations=classpath:db/migration/engine"
})
@Import(ReportApiTest.Containers.class)
class ReportApiTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    static final String DEFINITION = """
            {"id":"def-1","name":"Bank vs Ledger",
             "left":{"name":"Bank","format":"csv"},"right":{"name":"Ledger","format":"csv"},
             "matchFields":["ref"],"compareRules":[{"field":"amount","comparator":"number"}]}""";

    @Autowired
    ReportController reports;

    @Autowired
    JdbcClient jdbc;

    UUID runId;

    @BeforeEach
    void completedRun() {
        runId = UUID.randomUUID();
        jdbc.sql("""
                insert into engine.recon_run (id, definition_id, definition_name, definition, left_file_id, right_file_id,
                                              status, bucket_count, matched, mismatched, only_in_left, only_in_right)
                values (:id, 'def-1', 'Bank vs Ledger', cast(:def as jsonb), 'f1', 'f2', 'COMPLETED', 4, 2, 1, 1, 0)
                """).param("id", runId).param("def", DEFINITION).update();
        result("T1", "MATCHED", "{\"ref\":\"T1\",\"amount\":\"10\"}", "{\"ref\":\"T1\",\"amount\":\"10\"}", "[]");
        result("T2", "MATCHED", "{\"ref\":\"T2\",\"amount\":\"20\"}", "{\"ref\":\"T2\",\"amount\":\"20\"}", "[]");
        result("T3", "MISMATCHED", "{\"ref\":\"T3\",\"amount\":\"30\"}", "{\"ref\":\"T3\",\"amount\":\"35\"}",
                "[{\"field\":\"amount\",\"leftValue\":\"30\",\"rightValue\":\"35\",\"comparator\":\"number\"}]");
        result("T4", "ONLY_IN_LEFT", "{\"ref\":\"T4\",\"amount\":\"40\"}", null, "[]");
    }

    @Test
    void summaryShowsCountsAndMatchRate() {
        RunSummary summary = reports.summary(runId);
        assertThat(summary.matched()).isEqualTo(2);
        assertThat(summary.matchRatePercent()).isEqualTo(50.0);
        assertThat(summary.fields()).containsExactly("ref", "amount");
    }

    @Test
    void resultsArePagedAndFiltered() {
        assertThat(reports.results(runId, null, 0, 3).items()).hasSize(3);
        ResultPage mismatched = reports.results(runId, MatchStatus.MISMATCHED, 0, 50);
        assertThat(mismatched.total()).isEqualTo(1);
        assertThat(mismatched.items().getFirst().differences().getFirst().rightValue()).isEqualTo("35");
    }

    @Test
    void exportStreamsACsvFile() throws IOException {
        var response = reports.export(runId, "csv", null);
        var out = new ByteArrayOutputStream();
        response.getBody().writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(5);
        assertThat(lines[0]).isEqualTo("match_key,status,differences,left_ref,left_amount,right_ref,right_amount");
        assertThat(csv).contains("T3,MISMATCHED,amount: 30 vs 35,T3,30,T3,35");
    }

    @Test
    void unknownRunIsNotFound() {
        assertThatThrownBy(() -> reports.summary(UUID.randomUUID())).isInstanceOf(ResponseStatusException.class);
    }

    private void result(String key, String status, String left, String right, String differences) {
        jdbc.sql("""
                insert into engine.recon_result (run_id, bucket, match_key, status, left_fields, right_fields, differences)
                values (:run, 0, :key, :status, cast(:left as jsonb), cast(:right as jsonb), cast(:diff as jsonb))
                """)
                .param("run", runId).param("key", key).param("status", status)
                .param("left", left).param("right", right).param("diff", differences)
                .update();
    }
}
