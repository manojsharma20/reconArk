package io.reconark.ingestion;

import io.reconark.common.model.Side;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code ingestion.uploaded_file}: one row per upload. */
@Repository
public class FileRepository {

    private final JdbcClient jdbc;

    public FileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UploadedFile file) {
        jdbc.sql("""
                insert into ingestion.uploaded_file (id, definition_id, side, original_name, storage_key, size_bytes, sha256)
                values (:id, :definitionId, :side, :name, :key, :size, :sha)
                """)
                .param("id", file.id())
                .param("definitionId", file.definitionId())
                .param("side", file.side().name())
                .param("name", file.originalName())
                .param("key", file.storageKey())
                .param("size", file.sizeBytes())
                .param("sha", file.sha256())
                .update();
    }

    public Optional<UploadedFile> findById(String id) {
        return jdbc.sql("select * from ingestion.uploaded_file where id = :id").param("id", id).query(this::map).optional();
    }

    /** Latest uploads first; both filters are optional. */
    public List<UploadedFile> find(String definitionId, Side side) {
        return jdbc.sql("""
                select * from ingestion.uploaded_file
                where (cast(:definitionId as text) is null or definition_id = :definitionId)
                  and (cast(:side as text) is null or side = :side)
                order by uploaded_at desc
                limit 200
                """)
                .param("definitionId", definitionId)
                .param("side", side == null ? null : side.name())
                .query(this::map)
                .list();
    }

    private UploadedFile map(ResultSet rs, int row) throws SQLException {
        return new UploadedFile(
                rs.getString("id"),
                rs.getString("definition_id"),
                Side.valueOf(rs.getString("side")),
                rs.getString("original_name"),
                rs.getString("storage_key"),
                rs.getLong("size_bytes"),
                rs.getString("sha256"),
                rs.getObject("uploaded_at", OffsetDateTime.class).toInstant());
    }
}
