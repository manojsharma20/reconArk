package io.reconark.manager.run;

import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.Side;
import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.JobResultMessage;
import io.reconark.engine.messages.JobType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** All SQL for runs and jobs (schema {@code engine}). Plain SQL with Spring's {@link JdbcClient}. */
@Repository
public class RunRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public RunRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---------------------------------------------------------------- runs

    public void insertRun(UUID id, ReconDefinition definition, String leftFileId, String rightFileId, int bucketCount) {
        jdbc.sql("""
                insert into engine.recon_run (id, definition_id, definition_name, definition, left_file_id, right_file_id,
                                              status, bucket_count)
                values (:id, :definitionId, :name, cast(:definition as jsonb), :left, :right, :status, :buckets)
                """)
                .param("id", id)
                .param("definitionId", definition.id())
                .param("name", definition.name())
                .param("definition", json.writeValueAsString(definition))
                .param("left", leftFileId)
                .param("right", rightFileId)
                .param("status", RunStatus.LOADING.name())
                .param("buckets", bucketCount)
                .update();
    }

    public Optional<Run> findRun(UUID id) {
        return jdbc.sql("select * from engine.recon_run where id = :id").param("id", id).query(this::mapRun).optional();
    }

    /** Reads the run and locks its row until the transaction ends, so two job results for one run never race. */
    public Optional<Run> lockRun(UUID id) {
        return jdbc.sql("select * from engine.recon_run where id = :id for update").param("id", id).query(this::mapRun).optional();
    }

    public List<Run> latestRuns(int limit) {
        return jdbc.sql("select * from engine.recon_run order by created_at desc limit :limit")
                .param("limit", limit)
                .query(this::mapRun)
                .list();
    }

    public void updateRunStatus(UUID id, RunStatus status) {
        jdbc.sql("update engine.recon_run set status = :status where id = :id")
                .param("status", status.name())
                .param("id", id)
                .update();
    }

    /** Copies the LOAD totals from the jobs to the run. */
    public void recordLoadTotals(UUID runId) {
        jdbc.sql("""
                update engine.recon_run r set
                    left_loaded  = coalesce((select sum(records_read - records_rejected) from engine.recon_job
                                              where run_id = r.id and type = 'LOAD' and side = 'LEFT'), 0),
                    right_loaded = coalesce((select sum(records_read - records_rejected) from engine.recon_job
                                              where run_id = r.id and type = 'LOAD' and side = 'RIGHT'), 0),
                    rejected     = coalesce((select sum(records_rejected) from engine.recon_job
                                              where run_id = r.id and type = 'LOAD'), 0)
                where r.id = :id
                """).param("id", runId).update();
    }

    /** Counts the results per status, stores the counts and marks the run COMPLETED. */
    public void completeRun(UUID runId) {
        jdbc.sql("""
                update engine.recon_run r set
                    matched       = (select count(*) from engine.recon_result where run_id = r.id and status = 'MATCHED'),
                    mismatched    = (select count(*) from engine.recon_result where run_id = r.id and status = 'MISMATCHED'),
                    only_in_left  = (select count(*) from engine.recon_result where run_id = r.id and status = 'ONLY_IN_LEFT'),
                    only_in_right = (select count(*) from engine.recon_result where run_id = r.id and status = 'ONLY_IN_RIGHT'),
                    status = 'COMPLETED',
                    finished_at = now()
                where r.id = :id
                """).param("id", runId).update();
    }

    public void failRun(UUID runId, String error) {
        jdbc.sql("update engine.recon_run set status = 'FAILED', error = :error, finished_at = now() where id = :id")
                .param("error", error)
                .param("id", runId)
                .update();
    }

    /** Loaded records are only needed while matching; results keep both sides. */
    public int deleteStagingRecords(UUID runId) {
        return jdbc.sql("delete from engine.recon_record where run_id = :id").param("id", runId).update();
    }

    public long countRunsWithStatus(RunStatus status) {
        return jdbc.sql("select count(*) from engine.recon_run where status = :status")
                .param("status", status.name())
                .query(Long.class)
                .single();
    }

    // ---------------------------------------------------------------- jobs

    public void insertJob(JobMessage message) {
        jdbc.sql("""
                insert into engine.recon_job (id, run_id, type, side, storage_key, bucket, status, attempts)
                values (:id, :runId, :type, :side, :storageKey, :bucket, 'QUEUED', :attempt)
                """)
                .param("id", message.jobId())
                .param("runId", message.runId())
                .param("type", message.type().name())
                .param("side", message.side() == null ? null : message.side().name())
                .param("storageKey", message.storageKey())
                .param("bucket", message.bucket())
                .param("attempt", message.attempt())
                .update();
    }

    public Optional<Job> findJob(UUID id) {
        return jdbc.sql("select * from engine.recon_job where id = :id").param("id", id).query(this::mapJob).optional();
    }

    public List<Job> jobsOfRun(UUID runId) {
        return jdbc.sql("select * from engine.recon_job where run_id = :runId order by type, side, bucket")
                .param("runId", runId)
                .query(this::mapJob)
                .list();
    }

    public void completeJob(JobResultMessage result) {
        jdbc.sql("""
                update engine.recon_job set status = 'DONE', records_read = :read, records_rejected = :rejected,
                       results_written = :results, worker_id = :worker, error = null, finished_at = now()
                where id = :id
                """)
                .param("read", result.recordsRead())
                .param("rejected", result.recordsRejected())
                .param("results", result.resultsWritten())
                .param("worker", result.workerId())
                .param("id", result.jobId())
                .update();
    }

    /** Marks the job for another try and returns the new attempt number. */
    public int retryJob(UUID jobId, String lastError) {
        return jdbc.sql("""
                update engine.recon_job set attempts = attempts + 1, sent_at = now(), error = :error
                where id = :id returning attempts
                """)
                .param("error", lastError)
                .param("id", jobId)
                .query(Integer.class)
                .single();
    }

    public void failJob(UUID jobId, String error, String workerId) {
        jdbc.sql("update engine.recon_job set status = 'FAILED', error = :error, worker_id = :worker, finished_at = now() where id = :id")
                .param("error", error)
                .param("worker", workerId)
                .param("id", jobId)
                .update();
    }

    /** How many jobs of this type are not DONE yet. */
    public long countUnfinishedJobs(UUID runId, JobType type) {
        return jdbc.sql("select count(*) from engine.recon_job where run_id = :runId and type = :type and status <> 'DONE'")
                .param("runId", runId)
                .param("type", type.name())
                .query(Long.class)
                .single();
    }

    public long countQueuedJobs() {
        return jdbc.sql("select count(*) from engine.recon_job where status = 'QUEUED'").query(Long.class).single();
    }

    /**
     * Jobs that were sent before {@code sentBefore} and never answered, locked for this transaction. {@code SKIP LOCKED}
     * lets several manager instances share the work without picking the same job.
     */
    public List<Job> lockStuckJobs(Instant sentBefore, int limit) {
        return jdbc.sql("""
                select j.* from engine.recon_job j join engine.recon_run r on r.id = j.run_id
                where j.status = 'QUEUED' and j.sent_at < :before and r.status in ('LOADING', 'MATCHING')
                order by j.sent_at
                limit :limit
                for update of j skip locked
                """)
                .param("before", OffsetDateTime.ofInstant(sentBefore, java.time.ZoneOffset.UTC))
                .param("limit", limit)
                .query(this::mapJob)
                .list();
    }

    // ---------------------------------------------------------------- row mapping

    private Run mapRun(ResultSet rs, int row) throws SQLException {
        return new Run(
                rs.getObject("id", UUID.class),
                rs.getString("definition_id"),
                rs.getString("definition_name"),
                json.readValue(rs.getString("definition"), ReconDefinition.class),
                rs.getString("left_file_id"),
                rs.getString("right_file_id"),
                RunStatus.valueOf(rs.getString("status")),
                rs.getInt("bucket_count"),
                rs.getLong("left_loaded"),
                rs.getLong("right_loaded"),
                rs.getLong("rejected"),
                rs.getLong("matched"),
                rs.getLong("mismatched"),
                rs.getLong("only_in_left"),
                rs.getLong("only_in_right"),
                rs.getString("error"),
                instant(rs, "created_at"),
                instant(rs, "finished_at"));
    }

    private Job mapJob(ResultSet rs, int row) throws SQLException {
        String side = rs.getString("side");
        return new Job(
                rs.getObject("id", UUID.class),
                rs.getObject("run_id", UUID.class),
                JobType.valueOf(rs.getString("type")),
                side == null ? null : Side.valueOf(side),
                rs.getString("storage_key"),
                rs.getObject("bucket", Integer.class),
                JobStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getLong("records_read"),
                rs.getLong("records_rejected"),
                rs.getLong("results_written"),
                rs.getString("worker_id"),
                rs.getString("error"),
                instant(rs, "sent_at"),
                instant(rs, "finished_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
