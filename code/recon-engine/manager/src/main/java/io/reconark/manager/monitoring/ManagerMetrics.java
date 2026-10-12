package io.reconark.manager.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reconark.manager.run.RunRepository;
import io.reconark.manager.run.RunStatus;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Business metrics, scraped by Prometheus from {@code /actuator/prometheus} and shown in the Grafana dashboard.
 * Prometheus names: {@code reconark_runs_started_total}, {@code reconark_runs_finished_total{outcome}},
 * {@code reconark_jobs_retried_total}, {@code reconark_run_duration_seconds}, {@code reconark_jobs_queued},
 * {@code reconark_runs_active{status}}.
 */
@Component
public class ManagerMetrics {

    private final Counter started;
    private final Counter completed;
    private final Counter failed;
    private final Counter retried;
    private final Timer duration;

    public ManagerMetrics(MeterRegistry registry, RunRepository runs) {
        started = Counter.builder("reconark.runs.started").description("Runs started").register(registry);
        completed = Counter.builder("reconark.runs.finished").tag("outcome", "completed").register(registry);
        failed = Counter.builder("reconark.runs.finished").tag("outcome", "failed").register(registry);
        retried = Counter.builder("reconark.jobs.retried").description("Jobs sent again after a failure or timeout").register(registry);
        duration = Timer.builder("reconark.run.duration").description("Time from start to completion of a run").register(registry);

        Gauge.builder("reconark.jobs.queued", runs, RunRepository::countQueuedJobs)
                .description("Jobs sent to workers and not answered yet")
                .register(registry);
        for (RunStatus status : new RunStatus[] {RunStatus.LOADING, RunStatus.MATCHING}) {
            Gauge.builder("reconark.runs.active", runs, r -> r.countRunsWithStatus(status))
                    .tag("status", status.name())
                    .register(registry);
        }
    }

    public void runStarted() {
        started.increment();
    }

    public void runCompleted(Duration took) {
        completed.increment();
        duration.record(took);
    }

    public void runFailed() {
        failed.increment();
    }

    public void jobRetried() {
        retried.increment();
    }
}
