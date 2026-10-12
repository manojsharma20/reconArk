package io.reconark.plugins.fixedwidth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Plugs in the fixed-width reader. Switch off with {@code reconark.plugins.format-fixed-width.enabled=false}. */
@AutoConfiguration
@ConditionalOnProperty(name = "reconark.plugins.format-fixed-width.enabled", havingValue = "true", matchIfMissing = true)
public class FixedWidthFormatPlugin {

    @Bean
    FixedWidthFileFormat fixedWidthFileFormat() {
        return new FixedWidthFileFormat();
    }
}
