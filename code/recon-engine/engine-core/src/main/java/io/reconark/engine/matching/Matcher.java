package io.reconark.engine.matching;

import io.reconark.common.model.CompareRule;
import io.reconark.common.model.Difference;
import io.reconark.common.model.MatchStatus;
import io.reconark.common.model.ResultRow;
import io.reconark.common.model.Side;
import io.reconark.common.plugin.FieldComparator;
import io.reconark.common.plugin.PluginRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Matches the records of one bucket.
 *
 * <ol>
 *   <li>Group left and right records by match key.</li>
 *   <li>For each key, pair the 1st left with the 1st right, the 2nd with the 2nd, and so on (file order).</li>
 *   <li>Compare each pair with the compare rules: all equal = MATCHED, otherwise MISMATCHED with the differences.</li>
 *   <li>Records left without a partner become ONLY_IN_LEFT or ONLY_IN_RIGHT.</li>
 * </ol>
 * Results come out sorted by match key, so the same input always gives the same output.
 */
public class Matcher {

    private final PluginRegistry<FieldComparator> comparators;

    public Matcher(PluginRegistry<FieldComparator> comparators) {
        this.comparators = comparators;
    }

    public List<ResultRow> match(List<LoadedRecord> records, List<CompareRule> rules) {
        Map<String, List<LoadedRecord>> left = new TreeMap<>();
        Map<String, List<LoadedRecord>> right = new TreeMap<>();
        records.stream()
                .sorted(Comparator.comparingLong(LoadedRecord::lineNumber))
                .forEach(r -> (r.side() == Side.LEFT ? left : right)
                        .computeIfAbsent(r.matchKey(), k -> new ArrayList<>())
                        .add(r));

        var keys = new java.util.TreeSet<>(left.keySet());
        keys.addAll(right.keySet());

        List<ResultRow> results = new ArrayList<>();
        for (String key : keys) {
            List<LoadedRecord> l = left.getOrDefault(key, List.of());
            List<LoadedRecord> r = right.getOrDefault(key, List.of());
            int pairs = Math.min(l.size(), r.size());
            for (int i = 0; i < pairs; i++) {
                results.add(compare(key, l.get(i), r.get(i), rules));
            }
            for (int i = pairs; i < l.size(); i++) {
                results.add(new ResultRow(key, MatchStatus.ONLY_IN_LEFT, l.get(i).fields(), null, List.of()));
            }
            for (int i = pairs; i < r.size(); i++) {
                results.add(new ResultRow(key, MatchStatus.ONLY_IN_RIGHT, null, r.get(i).fields(), List.of()));
            }
        }
        return results;
    }

    private ResultRow compare(String key, LoadedRecord left, LoadedRecord right, List<CompareRule> rules) {
        List<Difference> differences = new ArrayList<>();
        for (CompareRule rule : rules) {
            String l = left.fields().get(rule.field());
            String r = right.fields().get(rule.field());
            if (!comparators.get(rule.comparator()).same(l, r, rule.options())) {
                differences.add(new Difference(rule.field(), l, r, rule.comparator()));
            }
        }
        MatchStatus status = differences.isEmpty() ? MatchStatus.MATCHED : MatchStatus.MISMATCHED;
        return new ResultRow(key, status, left.fields(), right.fields(), differences);
    }
}
