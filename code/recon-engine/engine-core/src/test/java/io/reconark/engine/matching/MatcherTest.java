package io.reconark.engine.matching;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.common.model.CompareRule;
import io.reconark.common.model.MatchStatus;
import io.reconark.common.model.ResultRow;
import io.reconark.common.model.Side;
import io.reconark.common.plugin.FieldComparator;
import io.reconark.common.plugin.PluginRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MatcherTest {

    /** A tiny comparator so this test does not depend on the plugin modules. */
    static final FieldComparator NUMBER = new FieldComparator() {
        public String id() {
            return "number";
        }

        public String description() {
            return "test";
        }

        public boolean same(String l, String r, Map<String, String> o) {
            return new BigDecimal(l).compareTo(new BigDecimal(r)) == 0;
        }
    };

    private final Matcher matcher = new Matcher(new PluginRegistry<>("comparator", List.of(NUMBER)));
    private final List<CompareRule> rules = List.of(new CompareRule("amount", "number", Map.of()));

    private static LoadedRecord rec(Side side, String key, long line, String amount) {
        return new LoadedRecord(side, key, line, Map.of("ref", key, "amount", amount));
    }

    @Test
    void classifiesEveryKey() {
        List<ResultRow> results = matcher.match(List.of(
                rec(Side.LEFT, "T1", 1, "10.00"), rec(Side.RIGHT, "T1", 1, "10.0"),   // matched
                rec(Side.LEFT, "T2", 2, "20.00"), rec(Side.RIGHT, "T2", 2, "25.00"),  // mismatched
                rec(Side.LEFT, "T3", 3, "30.00"),                                       // only left
                rec(Side.RIGHT, "T4", 3, "40.00")), rules);                             // only right

        assertThat(results).extracting(ResultRow::matchKey, ResultRow::status).containsExactly(
                org.assertj.core.groups.Tuple.tuple("T1", MatchStatus.MATCHED),
                org.assertj.core.groups.Tuple.tuple("T2", MatchStatus.MISMATCHED),
                org.assertj.core.groups.Tuple.tuple("T3", MatchStatus.ONLY_IN_LEFT),
                org.assertj.core.groups.Tuple.tuple("T4", MatchStatus.ONLY_IN_RIGHT));
        assertThat(results.get(1).differences()).singleElement()
                .satisfies(d -> assertThat(d.field()).isEqualTo("amount"))
                .satisfies(d -> assertThat(d.leftValue()).isEqualTo("20.00"))
                .satisfies(d -> assertThat(d.rightValue()).isEqualTo("25.00"));
    }

    @Test
    void duplicatesArePairedInFileOrderAndExtrasAreUnmatched() {
        List<ResultRow> results = matcher.match(List.of(
                rec(Side.LEFT, "T1", 2, "11.00"), rec(Side.LEFT, "T1", 1, "10.00"), rec(Side.LEFT, "T1", 3, "12.00"),
                rec(Side.RIGHT, "T1", 1, "10.00"), rec(Side.RIGHT, "T1", 2, "11.00")), rules);

        assertThat(results).extracting(ResultRow::status)
                .containsExactly(MatchStatus.MATCHED, MatchStatus.MATCHED, MatchStatus.ONLY_IN_LEFT);
        assertThat(results.get(2).left()).containsEntry("amount", "12.00");
    }
}
