package io.reconark.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reconark.common.model.CompareRule;
import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.SourceSpec;
import io.reconark.common.plugin.PluginInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Exercises the controllers against a real PostgreSQL (Testcontainers). */
@SpringBootTest
@Import(DefinitionApiTest.Containers.class)
class DefinitionApiTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    @Autowired
    DefinitionController definitions;

    @Autowired
    PluginCatalogController plugins;

    static ReconDefinition sample(String name) {
        return new ReconDefinition(null, name,
                new SourceSpec("Bank", "csv", Map.of(), Map.of("reference", "REF")),
                new SourceSpec("Ledger", "fixed-width", Map.of("columns", "reference:1:10,amount:11:10"), Map.of()),
                List.of("reference"),
                List.of(new CompareRule("amount", "number", Map.of("tolerance", "0.01"))));
    }

    @Test
    void createReadUpdateDelete() {
        ReconDefinition created = definitions.create(sample("Bank vs Ledger"));
        assertThat(created.id()).isNotBlank();
        assertThat(definitions.one(created.id()).left().fieldMapping()).containsEntry("reference", "REF");

        ReconDefinition renamed = new ReconDefinition(null, "Bank vs GL", created.left(), created.right(),
                created.matchFields(), created.compareRules());
        assertThat(definitions.update(created.id(), renamed).name()).isEqualTo("Bank vs GL");
        assertThat(definitions.all()).extracting(ReconDefinition::name).contains("Bank vs GL");

        definitions.delete(created.id());
        assertThatThrownBy(() -> definitions.one(created.id())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void invalidDefinitionsListEveryProblem() {
        var bad = new ReconDefinition(null, "", new SourceSpec("A", "xml", Map.of(), Map.of()), null, List.of(),
                List.of(new CompareRule("amount", "fuzzy", Map.of())));
        var result = definitions.validate(bad);
        assertThat(result.valid()).isFalse();
        assertThat(result.problems()).hasSize(5).anyMatch(p -> p.contains("'xml'")).anyMatch(p -> p.contains("'fuzzy'"));
        assertThatThrownBy(() -> definitions.create(bad)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void namesAreUnique() {
        definitions.create(sample("Cards"));
        assertThatThrownBy(() -> definitions.create(sample("CARDS")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already called");
    }

    @Test
    void catalogueListsTheActivePlugins() {
        assertThat(plugins.plugins()).extracting(PluginInfo::id)
                .contains("csv", "fixed-width", "exact", "ignore-case", "number", "date");
    }
}
