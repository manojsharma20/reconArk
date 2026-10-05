package io.reconark.kernel.pipeline;

import java.util.List;
import java.util.Objects;

/**
 * An ordered stage graph, declared in a provider configuration version.
 *
 * @param id identifier, e.g. {@code acquirer-a/v7/etl}
 * @param stages stages in execution order
 */
public record PipelineDefinition(String id, List<StageDefinition> stages) {
    public PipelineDefinition {
        Objects.requireNonNull(id, "id");
        stages = List.copyOf(stages);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("A pipeline needs at least one stage");
        }
    }
}
