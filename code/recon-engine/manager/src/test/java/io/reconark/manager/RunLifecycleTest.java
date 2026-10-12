package io.reconark.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reconark.common.model.CompareRule;
import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.Side;
import io.reconark.common.model.SourceSpec;
import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.JobResultMessage;
import io.reconark.engine.messages.JobType;
import io.reconark.manager.clients.FileInfo;
import io.reconark.manager.clients.ServiceClients;
import io.reconark.manager.messaging.JobPublisher;
import io.reconark.manager.run.Run;
import io.reconark.manager.run.RunService;
import io.reconark.manager.run.RunStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Drives a run through its whole life against a real database. Kafka is replaced by a mock publisher, and the test
 * plays the part of the workers by calling {@link RunService#onJobResult} directly.
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "reconark.manager.bucket-count=4",
        "reconark.manager.max-attempts=2",
        "reconark.manager.watchdog-interval-ms=3600000"
})
@Import(PostgresTestConfig.class)
class RunLifecycleTest {

    static final ReconDefinition DEFINITION = new ReconDefinition("def-1", "Bank vs Ledger",
            new SourceSpec("Bank", "csv", Map.of(), Map.of()),
            new SourceSpec("Ledger", "csv", Map.of(), Map.of()),
            List.of("ref"), List.of(new CompareRule("amount", "number", Map.of())));

    @Autowired
    RunService runs;

    @Autowired
    JdbcClient jdbc;

    @MockitoBean
    ServiceClients clients;

    @MockitoBean
    JobPublisher publisher;

    @BeforeEach
    void inputs() {
        when(clients.definition("def-1")).thenReturn(DEFINITION);
        when(clients.file("f-left")).thenReturn(new FileInfo("f-left", "def-1", Side.LEFT, "bank.csv", "k/left", 10));
        when(clients.file("f-right")).thenReturn(new FileInfo("f-right", "def-1", Side.RIGHT, "ledger.csv", "k/right", 10));
    }

    @Test
    void runGoesFromLoadingToMatchingToCompleted() {
        Run run = runs.startRun("def-1", "f-left", "f-right");
        assertThat(run.status()).isEqualTo(RunStatus.LOADING);
        List<JobMessage> loads = sent(2);
        assertThat(loads).extracting(JobMessage::type).containsOnly(JobType.LOAD);

        clearInvocations(publisher);
        loads.forEach(job -> runs.onJobResult(success(job, 5)));
        assertThat(runs.getRun(run.id()).status()).isEqualTo(RunStatus.MATCHING);
        assertThat(runs.getRun(run.id()).leftLoaded()).isEqualTo(5);

        List<JobMessage> matches = sent(4);
        assertThat(matches).extracting(JobMessage::bucket).containsExactlyInAnyOrder(0, 1, 2, 3);

        // The "workers" write results for each bucket, then report success.
        for (JobMessage job : matches) {
            insertResult(job, "MATCHED");
            runs.onJobResult(success(job, 2));
        }

        Run done = runs.getRun(run.id());
        assertThat(done.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(done.matched()).isEqualTo(4);
        assertThat(done.finishedAt()).isNotNull();
    }

    @Test
    void duplicateResultsAreIgnored() {
        Run run = runs.startRun("def-1", "f-left", "f-right");
        JobMessage first = sent(2).getFirst();
        runs.onJobResult(success(first, 5));
        runs.onJobResult(success(first, 5)); // delivered twice by Kafka
        assertThat(runs.getRun(run.id()).status()).isEqualTo(RunStatus.LOADING);
    }

    @Test
    void failedJobIsRetriedThenFailsTheRun() {
        Run run = runs.startRun("def-1", "f-left", "f-right");
        JobMessage job = sent(2).getFirst();

        clearInvocations(publisher);
        runs.onJobResult(failure(job, 1));
        JobMessage retry = sent(1).getFirst();
        assertThat(retry.attempt()).isEqualTo(2);
        assertThat(retry.jobId()).isEqualTo(job.jobId());

        runs.onJobResult(failure(job, 2));
        Run failed = runs.getRun(run.id());
        assertThat(failed.status()).isEqualTo(RunStatus.FAILED);
        assertThat(failed.error()).contains("file is broken");
    }

    private List<JobMessage> sent(int expected) {
        ArgumentCaptor<JobMessage> captor = ArgumentCaptor.forClass(JobMessage.class);
        verify(publisher, atLeastOnce()).publish(captor.capture());
        List<JobMessage> all = captor.getAllValues();
        assertThat(all).hasSize(expected);
        return all;
    }

    private static JobResultMessage success(JobMessage job, long records) {
        return new JobResultMessage(job.jobId(), job.runId(), job.type(), job.attempt(), true, records, 0, records, "test", 1, null);
    }

    private static JobResultMessage failure(JobMessage job, int attempt) {
        return new JobResultMessage(job.jobId(), job.runId(), job.type(), attempt, false, 0, 0, 0, "test", 1, "file is broken");
    }

    private void insertResult(JobMessage job, String status) {
        jdbc.sql("insert into engine.recon_result (run_id, bucket, match_key, status) values (:run, :bucket, :key, :status)")
                .param("run", job.runId())
                .param("bucket", job.bucket())
                .param("key", "K" + job.bucket())
                .param("status", status)
                .update();
    }
}
