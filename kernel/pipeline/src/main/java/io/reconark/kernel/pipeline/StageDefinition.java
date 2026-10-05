package io.reconark.kernel.pipeline;

import java.util.Map;
import java.util.Objects;

/**
 * One stage in a pipeline definition.
 *
 * @param key {@code pipeline-stage} extension key
 * @param options raw options, validated against {@link PipelineStage#options()}
 * @param onError error policy for exceptions
 */
public record StageDefinition(String key, Map<String, Object> options, ErrorPolicy onError) {
    public StageDefinition {
        Objects.requireNonNull(key, "key");
        options = options == null ? Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(options));
        onError = onError == null ? ErrorPolicy.REJECT_RECORD : onError;
    }

    public static StageDefinition of(String key, Map<String, Object> options) {
        return new StageDefinition(key, options, ErrorPolicy.REJECT_RECORD);
    }
}
