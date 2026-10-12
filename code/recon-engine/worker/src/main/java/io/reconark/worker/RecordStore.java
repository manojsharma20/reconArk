package io.reconark.worker;

import io.reconark.common.model.ResultRow;
import io.reconark.common.model.Side;
import io.reconark.engine.matching.LoadedRecord;
import io.reconark.engine.messages.JobMessage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** All SQL of the worker: staging records ({@code engine.recon_record}) and results ({@code engine.recon_result}). */
@Repository
public class RecordStore {

    private static final TypeReference<Map<String, String>> FIELDS = new TypeReference<>() {};

    /** A record about to be stored by a LOAD job. */
    public record NewRecord(String matchKey, int bucket, long lineNumber, Map<String, String> fields) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final JsonMapper json;

    public RecordStore(JdbcTemplate jdbc, TransactionTemplate transaction, JsonMapper json) {
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.json = json;
    }

    public void deleteLoadedBy(UUID loadJobId) {
        jdbc.update("delete from engine.recon_record where load_job_id = ?", loadJobId);
    }

    public void insert(JobMessage job, List<NewRecord> batch) {
        jdbc.batchUpdate("""
                insert into engine.recon_record (run_id, side, bucket, match_key, line_number, fields, load_job_id)
                values (?, ?, ?, ?, ?, cast(? as jsonb), ?)
                """, batch, batch.size(), (ps, r) -> {
            ps.setObject(1, job.runId());
            ps.setString(2, job.side().name());
            ps.setInt(3, r.bucket());
            ps.setString(4, r.matchKey());
            ps.setLong(5, r.lineNumber());
            ps.setString(6, json.writeValueAsString(r.fields()));
            ps.setObject(7, job.jobId());
        });
    }

    public List<LoadedRecord> recordsOfBucket(UUID runId, int bucket) {
        return jdbc.query("""
                select side, match_key, line_number, fields from engine.recon_record
                where run_id = ? and bucket = ?
                """, (rs, row) -> new LoadedRecord(
                        Side.valueOf(rs.getString("side")),
                        rs.getString("match_key"),
                        rs.getLong("line_number"),
                        json.readValue(rs.getString("fields"), FIELDS)),
                runId, bucket);
    }

    /** Deletes the bucket's old results (from an earlier attempt) and inserts the new ones, in one transaction. */
    public void replaceResults(UUID runId, int bucket, List<ResultRow> results) {
        transaction.executeWithoutResult(tx -> {
            jdbc.update("delete from engine.recon_result where run_id = ? and bucket = ?", runId, bucket);
            jdbc.batchUpdate("""
                    insert into engine.recon_result (run_id, bucket, match_key, status, left_fields, right_fields, differences)
                    values (?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), cast(? as jsonb))
                    """, results, 1_000, (ps, r) -> {
                ps.setObject(1, runId);
                ps.setInt(2, bucket);
                ps.setString(3, r.matchKey());
                ps.setString(4, r.status().name());
                ps.setString(5, r.left() == null ? null : json.writeValueAsString(r.left()));
                ps.setString(6, r.right() == null ? null : json.writeValueAsString(r.right()));
                ps.setString(7, json.writeValueAsString(r.differences()));
            });
        });
    }
}
