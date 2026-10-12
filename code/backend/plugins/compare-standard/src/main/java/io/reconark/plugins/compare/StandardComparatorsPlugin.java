package io.reconark.plugins.compare;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Plugs in the standard comparators. Switch off with {@code reconark.plugins.compare-standard.enabled=false}. */
@AutoConfiguration
@ConditionalOnProperty(name = "reconark.plugins.compare-standard.enabled", havingValue = "true", matchIfMissing = true)
public class StandardComparatorsPlugin {

    @Bean
    ExactComparator exactComparator() {
        return new ExactComparator();
    }

    @Bean
    IgnoreCaseComparator ignoreCaseComparator() {
        return new IgnoreCaseComparator();
    }

    @Bean
    NumberComparator numberComparator() {
        return new NumberComparator();
    }

    @Bean
    DateComparator dateComparator() {
        return new DateComparator();
    }
}
