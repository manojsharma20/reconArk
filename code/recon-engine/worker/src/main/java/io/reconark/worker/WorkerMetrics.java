package io.reconark.worker;

import io.micrometer.core.instrument.MeterRegistry;
import io.reconark.common.model.MatchStatus;
import io.reconark.common.model.ResultRow;
import io.reconark.common.model.Side;
import io.reconark.engine.messages.JobType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Worker metrics for Prometheus/Grafana. Prometheus names: {@code reconark_worker_jobs_seconds{type,outcome}},
 * {@code reconark_worker_records_loaded_total{side}}, {@code reconark_worker_records_rejected_total{side}},
 * {@code reconark_worker_results_total{status}}.
 */
@Component
public class WorkerMetrics {

    private final MeterRegistry registry;

    public WorkerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void jobFinished(JobType type, boolean success, long millis) {
        registry.timer("reconark.worker.jobs", "type", type.name(), "outcome", success ? "success" : "failure")
                .record(millis, TimeUnit.MILLISECONDS);
    }

    public void recordsLoaded(Side side, long loaded, long rejected) {
        registry.counter("reconark.worker.records.loaded", "side", side.name()).increment(loaded);
        registry.counter("reconark.worker.records.rejected", "side", side.name()).increment(rejected);
    }

    public void results(List<ResultRow> results) {
        Map<MatchStatus, Long> byStatus = results.stream()
                .collect(Collectors.groupingBy(ResultRow::status, Collectors.counting()));
        byStatus.forEach((status, count) ->
                registry.counter("reconark.worker.results", "status", status.name()).increment(count));
    }
}
