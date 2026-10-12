package io.reconark.report;

import io.reconark.common.model.Difference;
import io.reconark.common.model.MatchStatus;
import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.ResultRow;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Read-only queries on {@code engine.recon_run} and {@code engine.recon_result}. */
@Repository
public class ResultRepository {

    private static final TypeReference<Map<String, String>> FIELDS = new TypeReference<>() {};
    private static final TypeReference<List<Difference>> DIFFERENCES = new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper json;

    public ResultRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate, JsonMapper json) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
        this.json = json;
    }

    public Optional<RunSummary> summary(UUID runId) {
        return jdbc.sql("select * from engine.recon_run where id = :id")
                .param("id", runId)
                .query((rs, row) -> new RunSummary(
                        rs.getString("id"),
                        rs.getString("definition_name"),
                        rs.getString("status"),
                        rs.getLong("left_loaded"),
                        rs.getLong("right_loaded"),
                        rs.getLong("rejected"),
                        rs.getLong("matched"),
                        rs.getLong("mismatched"),
                        rs.getLong("only_in_left"),
                        rs.getLong("only_in_right"),
                        json.readValue(rs.getString("definition"), ReconDefinition.class).allFields(),
                        instant(rs, "created_at"),
                        instant(rs, "finished_at")))
                .optional();
    }

    public ResultPage page(UUID runId, MatchStatus status, int page, int size) {
        String filter = status == null ? "" : " and status = :status";
        long total = jdbc.sql("select count(*) from engine.recon_result where run_id = :run" + filter)
                .param("run", runId)
                .param("status", status == null ? null : status.name())
                .query(Long.class)
                .single();
        List<ResultRow> items = jdbc.sql("select * from engine.recon_result where run_id = :run" + filter
                        + " order by id limit :limit offset :offset")
                .param("run", runId)
                .param("status", status == null ? null : status.name())
                .param("limit", size)
                .param("offset", (long) page * size)
                .query(this::map)
                .list();
        return new ResultPage(page, size, total, items);
    }

    /** Receives the rows of an export one by one. */
    @FunctionalInterface
    public interface RowsHandler {
        void handle(Iterator<ResultRow> rows) throws IOException;
    }

    /**
     * Hands every result of a run to {@code handler} as an iterator. Rows come from the database 1 000 at a time, so
     * millions of results never sit in memory. PostgreSQL only streams inside a transaction, hence
     * {@code @Transactional}.
     */
    @Transactional(readOnly = true)
    public void stream(UUID runId, MatchStatus status, RowsHandler handler) throws IOException {
        String filter = status == null ? "" : " and status = ?";
        Object[] args = status == null ? new Object[] {runId} : new Object[] {runId, status.name()};
        var template = new JdbcTemplate(jdbcTemplate.getDataSource());
        template.setFetchSize(1_000);
        try (Stream<ResultRow> rows = template.queryForStream(
                "select * from engine.recon_result where run_id = ?" + filter + " order by id", this::map, args)) {
            handler.handle(rows.iterator());
        }
    }

    private ResultRow map(ResultSet rs, int row) throws SQLException {
        String left = rs.getString("left_fields");
        String right = rs.getString("right_fields");
        return new ResultRow(
                rs.getString("match_key"),
                MatchStatus.valueOf(rs.getString("status")),
                left == null ? null : json.readValue(left, FIELDS),
                right == null ? null : json.readValue(right, FIELDS),
                json.readValue(rs.getString("differences"), DIFFERENCES));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
