package io.reconark.common.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PluginRegistryTest {

    record Fake(String id) implements FieldComparator {
        public String description() {
            return "fake";
        }

        public boolean same(String l, String r, Map<String, String> o) {
            return true;
        }
    }

    @Test
    void findsPluginsById() {
        var registry = new PluginRegistry<FieldComparator>("comparator", List.of(new Fake("b"), new Fake("a")));
        assertThat(registry.get("a").id()).isEqualTo("a");
        assertThat(registry.all()).extracting(Plugin::id).containsExactly("a", "b");
    }

    @Test
    void unknownIdListsWhatIsAvailable() {
        var registry = new PluginRegistry<FieldComparator>("comparator", List.of(new Fake("exact")));
        assertThatThrownBy(() -> registry.get("fuzzy"))
                .isInstanceOf(UnknownPluginException.class)
                .hasMessageContaining("'fuzzy'")
                .hasMessageContaining("[exact]");
    }

    @Test
    void duplicateIdsAreRejected() {
        assertThatThrownBy(() -> new PluginRegistry<FieldComparator>("comparator", List.of(new Fake("x"), new Fake("x"))))
                .isInstanceOf(IllegalStateException.class);
    }
}
