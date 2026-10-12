package io.reconark.worker;

import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.SourceSpec;
import io.reconark.common.plugin.FileFormat;
import io.reconark.common.plugin.FileStore;
import io.reconark.common.plugin.PluginRegistry;
import io.reconark.engine.matching.MatchKeys;
import io.reconark.engine.messages.JobMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * LOAD job: read one file and store its rows as records, each tagged with its match key and bucket.
 *
 * <ol>
 *   <li>Delete anything an earlier attempt of this job stored (so a retry starts clean).</li>
 *   <li>Read the file with the format plugin named in the definition (csv, fixed-width ...).</li>
 *   <li>Rename columns to business fields, build the match key; rows without a match key are rejected.</li>
 *   <li>Insert the records in batches of {@value #BATCH_SIZE}.</li>
 * </ol>
 */
@Component
public class LoadJob {

    static final int BATCH_SIZE = 1_000;

    private final FileStore fileStore;
    private final PluginRegistry<FileFormat> formats;
    private final RecordStore records;
    private final WorkerMetrics metrics;

    public LoadJob(FileStore fileStore, PluginRegistry<FileFormat> formats, RecordStore records, WorkerMetrics metrics) {
        this.fileStore = fileStore;
        this.formats = formats;
        this.records = records;
        this.metrics = metrics;
    }

    public JobStats run(JobMessage job) throws IOException {
        ReconDefinition definition = job.definition();
        SourceSpec source = definition.source(job.side());
        FileFormat format = formats.get(source.format());

        records.deleteLoadedBy(job.jobId());

        List<RecordStore.NewRecord> batch = new ArrayList<>(BATCH_SIZE);
        long[] read = {0};
        long[] rejected = {0};
        try (InputStream content = fileStore.open(job.storageKey())) {
            format.read(content, source.formatOptions(), row -> {
                read[0]++;
                Map<String, String> fields = source.toBusinessFields(row);
                String key = MatchKeys.build(fields, definition.matchFields());
                if (key == null) {
                    rejected[0]++;
                    return;
                }
                batch.add(new RecordStore.NewRecord(key, MatchKeys.bucket(key, job.bucketCount()), read[0], fields));
                if (batch.size() == BATCH_SIZE) {
                    records.insert(job, batch);
                    batch.clear();
                }
            });
        }
        if (!batch.isEmpty()) {
            records.insert(job, batch);
        }
        metrics.recordsLoaded(job.side(), read[0] - rejected[0], rejected[0]);
        return new JobStats(read[0], rejected[0], 0);
    }
}
