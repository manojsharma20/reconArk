package io.reconark.plugins.recon;

import io.reconark.domain.model.CanonicalRecord;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.spi.MatchGroup;
import io.reconark.spi.MatchStrategy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code one-to-one}: groups by normalised key. Groups with more than one record on a side are reported as they are;
 * the classifier marks them {@code DUPLICATE} (duplicate policy "flag both", the safe default).
 */
final class OneToOneStrategy implements MatchStrategy {

    @Override
    public List<MatchGroup> group(
            List<CanonicalRecord> left,
            List<CanonicalRecord> right,
            Function<CanonicalRecord, String> leftKey,
            Function<CanonicalRecord, String> rightKey,
            PluginConfig options) {
        Map<String, List<CanonicalRecord>> l = new LinkedHashMap<>();
        Map<String, List<CanonicalRecord>> r = new LinkedHashMap<>();
        left.forEach(rec -> l.computeIfAbsent(leftKey.apply(rec), k -> new ArrayList<>()).add(rec));
        right.forEach(rec -> r.computeIfAbsent(rightKey.apply(rec), k -> new ArrayList<>()).add(rec));
        List<MatchGroup> out = new ArrayList<>();
        l.forEach((k, ls) -> out.add(new MatchGroup(k, ls, r.getOrDefault(k, List.of()))));
        r.forEach((k, rs) -> {
            if (!l.containsKey(k)) {
                out.add(new MatchGroup(k, List.of(), rs));
            }
        });
        return out;
    }
}
