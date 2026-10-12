package io.reconark.worker;

import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.JobResultMessage;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Runs one job (LOAD or MATCH), times it, and turns the outcome, or any error, into a result message. */
@Component
public class JobHandler {

    private static final Logger log = LoggerFactory.getLogger(JobHandler.class);

    private final LoadJob loadJob;
    private final MatchJob matchJob;
    private final WorkerMetrics metrics;
    private final String workerId;

    public JobHandler(LoadJob loadJob, MatchJob matchJob, WorkerMetrics metrics, @Value("${HOSTNAME:worker}") String workerId) {
        this.loadJob = loadJob;
        this.matchJob = matchJob;
        this.metrics = metrics;
        this.workerId = workerId;
    }

    public JobResultMessage handle(JobMessage job) {
        long start = System.nanoTime();
        log.info("Job {} {} of run {} (attempt {}) started", job.type(), job.jobId(), job.runId(), job.attempt());
        try {
            JobStats stats = switch (job.type()) {
                case LOAD -> loadJob.run(job);
                case MATCH -> matchJob.run(job);
            };
            long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
            metrics.jobFinished(job.type(), true, millis);
            log.info("Job {} {} done in {} ms: {}", job.type(), job.jobId(), millis, stats);
            return new JobResultMessage(job.jobId(), job.runId(), job.type(), job.attempt(), true,
                    stats.recordsRead(), stats.recordsRejected(), stats.resultsWritten(), workerId, millis, null);
        } catch (Exception e) {
            long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
            metrics.jobFinished(job.type(), false, millis);
            log.warn("Job {} {} failed after {} ms", job.type(), job.jobId(), millis, e);
            return new JobResultMessage(job.jobId(), job.runId(), job.type(), job.attempt(), false,
                    0, 0, 0, workerId, millis, describe(e));
        }
    }

    /** A short error text for the run's status page. Messages name lines and fields, never data values. */
    private static String describe(Exception e) {
        String text = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
