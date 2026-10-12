package io.reconark.manager.run;

import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.Side;
import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.JobResultMessage;
import io.reconark.engine.messages.JobType;
import io.reconark.manager.ManagerProperties;
import io.reconark.manager.clients.FileInfo;
import io.reconark.manager.clients.ServiceClients;
import io.reconark.manager.messaging.JobPublisher;
import io.reconark.manager.monitoring.ManagerMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * The whole life of a run in one class.
 *
 * <pre>
 * start run ──► 2 LOAD jobs (left file, right file) ──► all LOAD done ──► N MATCH jobs (one per bucket)
 *           ──► all MATCH done ──► count results ──► COMPLETED
 * a job fails ──► send again (up to maxAttempts) ──► still failing ──► run FAILED
 * a job gets no answer within jobTimeout (worker died) ──► watchdog sends it again
 * </pre>
 *
 * <p>Rule: database changes happen in a transaction; Kafka messages are sent only <em>after</em> the transaction
 * commits. If the manager crashes in between, the job is QUEUED in the database and the watchdog sends it later.
 */
@Service
public class RunService {

    private static final Logger log = LoggerFactory.getLogger(RunService.class);

    private final RunRepository repository;
    private final ServiceClients clients;
    private final JobPublisher publisher;
    private final ManagerProperties properties;
    private final TransactionTemplate transaction;
    private final ManagerMetrics metrics;

    public RunService(RunRepository repository, ServiceClients clients, JobPublisher publisher,
            ManagerProperties properties, TransactionTemplate transaction, ManagerMetrics metrics) {
        this.repository = repository;
        this.clients = clients;
        this.publisher = publisher;
        this.properties = properties;
        this.transaction = transaction;
        this.metrics = metrics;
    }

    // ------------------------------------------------------------------ 1. start

    /** Starts a run: checks the inputs, stores the run and its two LOAD jobs, then sends the jobs. */
    public Run startRun(String definitionId, String leftFileId, String rightFileId) {
        ReconDefinition definition = clients.definition(definitionId);
        FileInfo left = clients.file(leftFileId);
        FileInfo right = clients.file(rightFileId);
        checkFile(left, definitionId, Side.LEFT);
        checkFile(right, definitionId, Side.RIGHT);

        UUID runId = UUID.randomUUID();
        int buckets = properties.bucketCount();
        List<JobMessage> jobs = List.of(
                JobMessage.load(UUID.randomUUID(), runId, 1, definition, buckets, Side.LEFT, left.storageKey()),
                JobMessage.load(UUID.randomUUID(), runId, 1, definition, buckets, Side.RIGHT, right.storageKey()));

        transaction.executeWithoutResult(tx -> {
            repository.insertRun(runId, definition, leftFileId, rightFileId, buckets);
            jobs.forEach(repository::insertJob);
        });
        jobs.forEach(publisher::publish);
        metrics.runStarted();
        log.info("Run {} started for '{}' ({} buckets)", runId, definition.name(), buckets);
        return repository.findRun(runId).orElseThrow();
    }

    private static void checkFile(FileInfo file, String definitionId, Side expectedSide) {
        if (!definitionId.equals(file.definitionId()) || file.side() != expectedSide) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File " + file.id() + " was uploaded as "
                    + file.side() + " for definition " + file.definitionId() + ", expected " + expectedSide
                    + " for definition " + definitionId);
        }
    }

    // ------------------------------------------------------------------ 2. job results

    /** Handles one job result from a worker. Safe to call twice with the same message. */
    public void onJobResult(JobResultMessage result) {
        Next next = transaction.execute(tx -> handle(result));
        if (next == null) {
            return;
        }
        next.toSend().forEach(publisher::publish);
        if (next.completedRun() != null && !properties.keepStagingRecords()) {
            int deleted = repository.deleteStagingRecords(next.completedRun());
            log.info("Run {} completed; removed {} staging records", next.completedRun(), deleted);
        }
    }

    /** What to do after the transaction: messages to send and, when a run just completed, its id. */
    private record Next(List<JobMessage> toSend, UUID completedRun) {
        static final Next NOTHING = new Next(List.of(), null);
    }

    private Next handle(JobResultMessage result) {
        Run run = repository.lockRun(result.runId()).orElse(null);
        if (run == null || run.status().isFinal()) {
            return Next.NOTHING; // unknown run, or it already finished: ignore
        }
        Job job = repository.findJob(result.jobId()).orElse(null);
        if (job == null || job.status() != JobStatus.QUEUED || job.attempts() != result.attempt()) {
            return Next.NOTHING; // duplicate message, or a late answer to an attempt we already gave up on
        }

        if (!result.success()) {
            return retryOrFail(run, job, result.error(), result.workerId());
        }

        repository.completeJob(result);

        if (job.type() == JobType.LOAD && repository.countUnfinishedJobs(run.id(), JobType.LOAD) == 0) {
            // Both files are loaded: split the matching into one job per bucket.
            repository.recordLoadTotals(run.id());
            List<JobMessage> matchJobs = new ArrayList<>();
            for (int bucket = 0; bucket < run.bucketCount(); bucket++) {
                matchJobs.add(JobMessage.match(UUID.randomUUID(), run.id(), 1, run.definition(), run.bucketCount(), bucket));
            }
            matchJobs.forEach(repository::insertJob);
            repository.updateRunStatus(run.id(), RunStatus.MATCHING);
            return new Next(matchJobs, null);
        }

        if (job.type() == JobType.MATCH && repository.countUnfinishedJobs(run.id(), JobType.MATCH) == 0) {
            repository.completeRun(run.id());
            metrics.runCompleted(Duration.between(run.createdAt(), Instant.now()));
            return new Next(List.of(), run.id());
        }
        return Next.NOTHING;
    }

    private Next retryOrFail(Run run, Job job, String error, String workerId) {
        if (job.attempts() < properties.maxAttempts()) {
            int attempt = repository.retryJob(job.id(), error);
            metrics.jobRetried();
            log.warn("Job {} ({}) of run {} failed on attempt {}: {}. Sending it again.",
                    job.id(), job.type(), run.id(), job.attempts(), error);
            return new Next(List.of(toMessage(job, run, attempt)), null);
        }
        repository.failJob(job.id(), error, workerId);
        repository.failRun(run.id(), job.type() + " job failed after " + job.attempts() + " attempts: " + error);
        metrics.runFailed();
        log.error("Run {} failed: {} job {} failed {} times: {}", run.id(), job.type(), job.id(), job.attempts(), error);
        return Next.NOTHING;
    }

    private static JobMessage toMessage(Job job, Run run, int attempt) {
        return job.type() == JobType.LOAD
                ? JobMessage.load(job.id(), run.id(), attempt, run.definition(), run.bucketCount(), job.side(), job.storageKey())
                : JobMessage.match(job.id(), run.id(), attempt, run.definition(), run.bucketCount(), job.bucket());
    }

    // ------------------------------------------------------------------ 3. watchdog

    /** Every 30 seconds: jobs without an answer after the job timeout are sent again (or fail the run). */
    @Scheduled(fixedDelayString = "${reconark.manager.watchdog-interval-ms:30000}")
    public void resendStuckJobs() {
        List<JobMessage> toSend = transaction.execute(tx -> {
            List<JobMessage> messages = new ArrayList<>();
            for (Job job : repository.lockStuckJobs(Instant.now().minus(properties.jobTimeout()), 100)) {
                Run run = repository.findRun(job.runId()).orElseThrow();
                messages.addAll(retryOrFail(run, job, "no answer within " + properties.jobTimeout(), null).toSend());
            }
            return messages;
        });
        if (toSend != null) {
            toSend.forEach(publisher::publish);
        }
    }

    // ------------------------------------------------------------------ queries

    public Run getRun(UUID id) {
        return repository.findRun(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + id));
    }

    public List<Run> latestRuns() {
        return repository.latestRuns(100);
    }

    public List<Job> jobs(UUID runId) {
        getRun(runId);
        return repository.jobsOfRun(runId);
    }
}
