package io.reconark.plugins.compare;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StandardComparatorsTest {

    @Test
    void exactIgnoresSurroundingSpacesAndTreatsMissingAsEmpty() {
        var exact = new ExactComparator();
        assertThat(exact.same(" A1 ", "A1", Map.of())).isTrue();
        assertThat(exact.same(null, "", Map.of())).isTrue();
        assertThat(exact.same("A1", "a1", Map.of())).isFalse();
    }

    @Test
    void ignoreCase() {
        assertThat(new IgnoreCaseComparator().same("Acme", "ACME", Map.of())).isTrue();
    }

    @Test
    void numbersWithAndWithoutTolerance() {
        var number = new NumberComparator();
        assertThat(number.same("10.0", "10.00", Map.of())).isTrue();
        assertThat(number.same("10.00", "10.01", Map.of())).isFalse();
        assertThat(number.same("10.00", "10.01", Map.of("tolerance", "0.01"))).isTrue();
        assertThat(number.same("-5", "5", Map.of("tolerance", "1"))).isFalse();
        assertThat(number.same("n/a", "n/a", Map.of())).isTrue();
        assertThat(number.same("n/a", "1", Map.of())).isFalse();
    }

    @Test
    void datesWithDifferentFormatsAndTolerance() {
        var date = new DateComparator();
        assertThat(date.same("20261001", "2026-10-01", Map.of("leftFormat", "yyyyMMdd"))).isTrue();
        assertThat(date.same("2026-10-01", "2026-10-02", Map.of())).isFalse();
        assertThat(date.same("2026-10-01", "2026-10-02", Map.of("toleranceDays", "1"))).isTrue();
    }
}
