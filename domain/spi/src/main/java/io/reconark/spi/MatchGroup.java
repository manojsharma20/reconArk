package io.reconark.spi;

import io.reconark.domain.model.CanonicalRecord;
import java.util.List;

/**
 * Records from both sides that a match strategy grouped together under one normalised key.
 *
 * @param key normalised match key
 * @param left left-side members
 * @param right right-side members
 */
public record MatchGroup(String key, List<CanonicalRecord> left, List<CanonicalRecord> right) {
    public MatchGroup {
        left = List.copyOf(left);
        right = List.copyOf(right);
    }
}
