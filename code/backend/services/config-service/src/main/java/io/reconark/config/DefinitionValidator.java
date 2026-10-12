package io.reconark.config;

import io.reconark.common.model.CompareRule;
import io.reconark.common.model.ReconDefinition;
import io.reconark.common.model.SourceSpec;
import io.reconark.common.plugin.FieldComparator;
import io.reconark.common.plugin.FileFormat;
import io.reconark.common.plugin.PluginRegistry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Checks a definition before it is saved, and returns every problem at once (not just the first) so the user can fix
 * them in one go.
 */
@Component
public class DefinitionValidator {

    private final PluginRegistry<FileFormat> formats;
    private final PluginRegistry<FieldComparator> comparators;

    public DefinitionValidator(PluginRegistry<FileFormat> formats, PluginRegistry<FieldComparator> comparators) {
        this.formats = formats;
        this.comparators = comparators;
    }

    public List<String> problems(ReconDefinition definition) {
        List<String> problems = new ArrayList<>();
        if (definition.name() == null || definition.name().isBlank()) {
            problems.add("name is required");
        } else if (definition.name().length() > 100) {
            problems.add("name must be at most 100 characters");
        }
        checkSource("left", definition.left(), problems);
        checkSource("right", definition.right(), problems);
        if (definition.matchFields().isEmpty()) {
            problems.add("matchFields needs at least one field");
        }
        if (definition.matchFields().stream().anyMatch(f -> f == null || f.isBlank())) {
            problems.add("matchFields must not contain empty names");
        }
        for (CompareRule rule : definition.compareRules()) {
            if (rule.field() == null || rule.field().isBlank()) {
                problems.add("every compare rule needs a field");
            }
            if (rule.comparator() == null || !comparators.contains(rule.comparator())) {
                problems.add("compare rule for '" + rule.field() + "' uses unknown comparator '" + rule.comparator()
                        + "'; available: " + ids(comparators));
            }
        }
        return problems;
    }

    private void checkSource(String side, SourceSpec source, List<String> problems) {
        if (source == null) {
            problems.add(side + " source is required");
            return;
        }
        if (source.name() == null || source.name().isBlank()) {
            problems.add(side + ".name is required");
        }
        if (source.format() == null || !formats.contains(source.format())) {
            problems.add(side + ".format '" + source.format() + "' is unknown; available: " + ids(formats));
        }
    }

    private static List<String> ids(PluginRegistry<?> registry) {
        return registry.all().stream().map(p -> p.id()).toList();
    }
}
