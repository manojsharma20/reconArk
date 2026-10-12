package io.reconark.config;

import io.reconark.common.model.ReconDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Stores definitions as JSON documents in {@code config.recon_definition}. */
@Repository
public class DefinitionRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public DefinitionRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<ReconDefinition> findAll() {
        return jdbc.sql("select definition from config.recon_definition order by name")
                .query((rs, row) -> json.readValue(rs.getString(1), ReconDefinition.class))
                .list();
    }

    public Optional<ReconDefinition> findById(String id) {
        return jdbc.sql("select definition from config.recon_definition where id = :id")
                .param("id", id)
                .query((rs, row) -> json.readValue(rs.getString(1), ReconDefinition.class))
                .optional();
    }

    public boolean nameTakenByAnother(String name, String id) {
        return jdbc.sql("select count(*) from config.recon_definition where lower(name) = lower(:name) and id <> :id")
                .param("name", name)
                .param("id", id)
                .query(Long.class)
                .single() > 0;
    }

    public void insert(ReconDefinition definition) {
        jdbc.sql("insert into config.recon_definition (id, name, definition) values (:id, :name, cast(:json as jsonb))")
                .param("id", definition.id())
                .param("name", definition.name())
                .param("json", json.writeValueAsString(definition))
                .update();
    }

    public boolean update(ReconDefinition definition) {
        return jdbc.sql("""
                update config.recon_definition set name = :name, definition = cast(:json as jsonb), updated_at = now()
                where id = :id
                """)
                .param("id", definition.id())
                .param("name", definition.name())
                .param("json", json.writeValueAsString(definition))
                .update() == 1;
    }

    public boolean delete(String id) {
        return jdbc.sql("delete from config.recon_definition where id = :id").param("id", id).update() == 1;
    }
}
