package io.reconark.engine.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MatchKeysTest {

    @Test
    void joinsTrimmedMatchFields() {
        assertThat(MatchKeys.build(Map.of("ref", " T1 ", "ccy", "AED"), List.of("ref", "ccy"))).isEqualTo("T1|AED");
    }

    @Test
    void missingOrBlankMatchFieldGivesNoKey() {
        assertThat(MatchKeys.build(Map.of("ref", " "), List.of("ref"))).isNull();
        assertThat(MatchKeys.build(Map.of(), List.of("ref"))).isNull();
    }

    @Test
    void sameKeyAlwaysGoesToTheSameBucketWithinRange() {
        for (String key : List.of("T1", "T2", "a-very-long-reference-0001", "")) {
            int bucket = MatchKeys.bucket(key, 16);
            assertThat(bucket).isBetween(0, 15).isEqualTo(MatchKeys.bucket(key, 16));
        }
    }
}
