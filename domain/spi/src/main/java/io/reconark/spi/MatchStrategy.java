package io.reconark.spi;

import io.reconark.domain.model.CanonicalRecord;
import io.reconark.kernel.api.PluginConfig;
import java.util.List;
import java.util.function.Function;

/** Pairs records across two sides (1:1, 1:N, N:1, fuzzy ...). */
public interface MatchStrategy {

    /**
     * @param left left-side candidates
     * @param right right-side candidates
     * @param leftKey normalised key of a left record
     * @param rightKey normalised key of a right record
     * @param options strategy options (duplicate policy, aggregation ...)
     */
    List<MatchGroup> group(
            List<CanonicalRecord> left,
            List<CanonicalRecord> right,
            Function<CanonicalRecord, String> leftKey,
            Function<CanonicalRecord, String> rightKey,
            PluginConfig options);
}
