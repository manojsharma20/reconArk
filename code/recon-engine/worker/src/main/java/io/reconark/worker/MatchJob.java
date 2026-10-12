package io.reconark.worker;

import io.reconark.common.model.ResultRow;
import io.reconark.engine.matching.LoadedRecord;
import io.reconark.engine.matching.Matcher;
import io.reconark.engine.messages.JobMessage;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MATCH job: match the left and right records of one bucket and store the results. Results of an earlier attempt for
 * the same bucket are replaced, so a retry never creates duplicates.
 */
@Component
public class MatchJob {

    private final RecordStore records;
    private final Matcher matcher;
    private final WorkerMetrics metrics;

    public MatchJob(RecordStore records, Matcher matcher, WorkerMetrics metrics) {
        this.records = records;
        this.matcher = matcher;
        this.metrics = metrics;
    }

    public JobStats run(JobMessage job) {
        List<LoadedRecord> bucket = records.recordsOfBucket(job.runId(), job.bucket());
        List<ResultRow> results = matcher.match(bucket, job.definition().compareRules());
        records.replaceResults(job.runId(), job.bucket(), results);
        metrics.results(results);
        return new JobStats(bucket.size(), 0, results.size());
    }
}
