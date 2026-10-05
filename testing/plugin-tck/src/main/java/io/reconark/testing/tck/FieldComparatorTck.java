package io.reconark.testing.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.runtime.ConfigValidator;
import io.reconark.spi.Comparison;
import io.reconark.spi.FieldComparator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Contract of {@code field-comparator}. */
public abstract class FieldComparatorTck {

    protected abstract FieldComparator comparator();

    /** Raw parameters for the comparator; validated against its spec. */
    protected Map<String, Object> parameters() {
        return Map.of();
    }

    /** Two values that must compare equal. */
    protected abstract Object[] equalPair();

    /** Two values that must compare different. */
    protected abstract Object[] differentPair();

    private PluginConfig params() {
        return ConfigValidator.validate("tck", comparator().parameters(), parameters());
    }

    @Test
    void equalValuesAreEqual() {
        Object[] p = equalPair();
        assertThat(comparator().compare(p[0], p[1], params()).equal()).isTrue();
    }

    @Test
    void differentValuesAreExplained() {
        Object[] p = differentPair();
        Comparison c = comparator().compare(p[0], p[1], params());
        assertThat(c.equal()).isFalse();
        assertThat(c.explanation()).isNotBlank();
    }

    @Test
    void reflexive() {
        Object v = equalPair()[0];
        assertThat(comparator().compare(v, v, params()).equal()).isTrue();
    }

    @Test
    void nullsNeverThrow() {
        Object v = equalPair()[0];
        assertThatCode(() -> {
            comparator().compare(null, v, params());
            comparator().compare(v, null, params());
            comparator().compare(null, null, params());
        }).doesNotThrowAnyException();
        assertThat(comparator().compare(null, v, params()).equal()).isFalse();
    }

    @Test
    void defaultParametersAreValid() {
        assertThatCode(this::params).doesNotThrowAnyException();
    }
}
